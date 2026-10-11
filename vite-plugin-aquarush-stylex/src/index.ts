'use strict'

/**
 * vite-plugin-aquarush-stylex
 *
 * 在 vite transform 阶段运行 @stylexjs/babel-plugin,把 stylex.create /
 * defineVars / createTheme 编译为原子类名与 CSS 变量,抽取出的 CSS 通过
 * 虚拟模块 virtual:stylex.css 进入构建管线(应用入口 import 一次)。
 *
 * 为什么编译期抽取而不是运行时注入:微信小程序没有 DOM,运行时
 * document.createElement('style') 不可用;Taro 官方对 CSS-in-JS 的结论相同
 * (必须编译期抽取为静态样式)。因此除 web dev(可选运行时注入便于调试)外,
 * 一律 dev:false 走抽取路径。
 *
 * 接线(两端一致):
 *   1. vite 插件列表首位(或 vitePlugins 首位):
 *        aquarushStylexVitePlugin({ dev: command === 'serve' })
 *      小程序端不传 dev(恒为抽取模式)。
 *   2. 应用入口: import 'virtual:stylex.css'
 *   3. 依赖: @stylexjs/stylex(runtime + 类型;版本须与 @stylexjs/babel-plugin 同步)
 *
 * 跨包 token(@aquarush/common/theme/tokens.stylex.ts)经
 * unstable_moduleResolution(commonJS)解析;rootDir 固定为仓库根,
 * 与进程 cwd 无关,保证 var 名在两端构建间稳定一致。
 *
 * 已知限制:小程序 --watch 增量编译下,新增/修改样式后虚拟模块可能不失效,
 * 需整包重建(taro build --type weapp);生产构建不受影响。
 */

import path from 'node:path'
import { transformSync } from '@babel/core'
// eslint-disable-next-line import/no-named-as-default
import stylexBabelPlugin from '@stylexjs/babel-plugin'

// 本包位于 <repo>/vite-plugin-aquarush-stylex/dist/index.js → 仓库根为上两级
const REPO_ROOT = path.resolve(__dirname, '..', '..')

export const STYLEX_VIRTUAL_CSS_ID = 'virtual:stylex.css'
const RESOLVED_VIRTUAL_ID = '\0' + STYLEX_VIRTUAL_CSS_ID

/**
 * 虚拟模块占位规则:load 发生在 import 图解析期(此时源文件尚未 transform、
 * CSS 还没收集),因此先放一个占位选择器随 css 管线走完(minify/postcss/
 * wxss 组装),最后在 generateBundle 用真实 CSS 整体替换该规则。
 * 选择器 + 非空声明保证不会被 css 压缩器丢弃。
 */
const CSS_SLOT_RULE = '.aquarush-stylex-slot{--aquarush-stylex-slot:1}'

export interface AquarushStylexVitePluginOptions {
  /**
   * dev 模式(仅 web 端):true 时走 stylex 运行时注入(<style> 标签),
   * 便于浏览器 DevTools 调试;false(默认)恒为编译期抽取。
   * 小程序端必须保持默认 false。
   */
  dev?: boolean
  /**
   * rpx→px 换算(÷2,750 设计稿基准→375 web 基准):
   * 供 taro h5 构建使用 —— stylex 抽取的 CSS 绕过了 taro 的 postcss
   * 尺寸管线,而浏览器不识别 rpx;开启后按 weapp 设计基准换算成 px。
   * admin(web)等本身不含 rpx 的构建保持默认 false。
   */
  convertRpx?: boolean
}

/** rollup bundle 里的 asset 形状(仅用到的字段) */
interface StylexBundleAsset {
  type: string
  facadeModuleId?: string | null
  fileName?: string
  source?: string | Uint8Array
}

/** Vite 插件最小结构化类型(避免引入 vite 依赖,与 babel-plugin-aquarush-track/vite 一致) */
export interface AquarushStylexVitePlugin {
  name: string
  enforce: 'pre'
  transform(code: string, id: string): { code: string; map: unknown } | null
  resolveId(id: string): string | null
  load(id: string): string | null
  generateBundle(
    this: { emitFile?: (file: { type: 'asset'; fileName: string; source: string }) => void },
    options: unknown,
    bundle: Record<string, StylexBundleAsset>,
  ): void
}

export function aquarushStylexVitePlugin(
  options: AquarushStylexVitePluginOptions = {},
): AquarushStylexVitePlugin {
  const dev = options.dev ?? false
  const convertRpx = options.convertRpx ?? false
  /** 模块绝对路径 → 该模块抽取出的 CSS(增量刷新,退出前一直保持) */
  const cssByModule = new Map<string, string>()

  /**
   * 汇总全部抽取 CSS。@media 规则放最后:同一原子类的媒体查询变体若先于
   * 基础规则出现,在-active 媒体条件下会被后者(源码序更后、同特异性)覆盖。
   */
  const collectCss = (): string => {
    const base: string[] = []
    const media: string[] = []
    for (const css of cssByModule.values()) {
      ;(css.includes('@media') ? media : base).push(css)
    }
    const joined = [...base, ...media].join('\n')
    if (!convertRpx) return joined
    // 750 设计稿基准 rpx → web px(÷2);hairline 1rpx → 0.5px
    return joined.replace(/(\d+(?:\.\d+)?)rpx/g, (_, n: string) => {
      const px = Number(n) / 2
      return `${Number.isInteger(px) ? px : px.toFixed(2)}px`
    })
  }

  return {
    name: 'aquarush-stylex',
    enforce: 'pre',
    transform(code: string, id: string) {      const file = id.split('?', 1)[0]
      if (!/\.[cm]?[jt]sx?$/.test(file)) return null
      if (file.startsWith('\0') || file.includes('node_modules')) return null
      // 快速路径:与 stylex 无关的文件零开销
      if (!code.includes('stylex')) return null

      const result = transformSync(code, {
        filename: file,
        babelrc: false,
        configFile: false,
        parserOpts: { plugins: ['jsx', 'typescript'] },
        plugins: [
          [
            stylexBabelPlugin,
            {
              dev,
              runtimeInjection: dev,
              unstable_moduleResolution: { type: 'commonJS', rootDir: REPO_ROOT },
            },
          ],
        ],
        sourceMaps: true,
      })
      if (!result) return null

      if (!dev) {
        // metadata.stylex 两种形状:
        //   普通文件:[cssString]
        //   主题文件(defineVars):[[themeHash, { ltr: cssString, rtl? }]]
        const meta = result.metadata as { stylex?: unknown } | undefined
        const entries = Array.isArray(meta?.stylex) ? (meta?.stylex as unknown[]) : null
        if (entries) {
          const parts: string[] = []
          for (const entry of entries) {
            if (typeof entry === 'string') {
              parts.push(entry)
            } else if (Array.isArray(entry) && typeof entry[1] === 'object' && entry[1] !== null) {
              for (const css of Object.values(entry[1] as Record<string, unknown>)) {
                if (typeof css === 'string') parts.push(css)
              }
            }
          }
          const css = parts.join('\n')
          if (css) cssByModule.set(file, css)
          else cssByModule.delete(file)
        }
      }
      return { code: result.code ?? code, map: result.map ?? null }
    },
    resolveId(id: string) {
      if (id === STYLEX_VIRTUAL_CSS_ID) return RESOLVED_VIRTUAL_ID
      return null
    },
    load(id: string) {
      if (id === RESOLVED_VIRTUAL_ID) return dev ? '' : CSS_SLOT_RULE
      return null
    },
    generateBundle(
      _options: unknown,
      bundle: Record<string, { type: string; isEntry?: boolean; facadeModuleId?: string | null; fileName?: string; source?: string | Uint8Array }>,
    ) {
      if (dev) return
      const css = collectCss()
      for (const asset of Object.values(bundle)) {
        if (asset.type !== 'asset' || typeof asset.source !== 'string') continue
        if (asset.source.includes(CSS_SLOT_RULE)) {
          asset.source = asset.source.split(CSS_SLOT_RULE).join(css)
          return
        }
      }
      // 兜底:没找到占位(理论上不该发生)——独立产出一份并告警
      this.emitFile?.({ type: 'asset', fileName: 'stylex.css', source: css })
      console.warn('[vite-plugin-aquarush-stylex] 未找到 virtual:stylex.css 的产物占位,CSS 已单发 stylex.css,请检查入口 import')
    },
  }
}

export default aquarushStylexVitePlugin
