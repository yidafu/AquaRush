'use strict'

/**
 * babel-plugin-aquarush-track 的 Vite 插件包装
 *
 * 用途:Taro 4 的 vite 编译器(compiler.type = 'vite')用 esbuild 转 JSX,
 * 不读取项目的 babel.config.js plugins —— 需要本插件在 vite transform 阶段
 * 提前把 track-* props 编译成 <Track> 包裹(输出仍保留 JSX,交给后续 esbuild 编译)。
 *
 * 接线(Taro config/index.ts):
 *   import { aquarushTrackVitePlugin } from 'babel-plugin-aquarush-track/vite'
 *   compiler: { type: 'vite', vitePlugins: [aquarushTrackVitePlugin()] }
 */

import { transformSync } from '@babel/core'
import trackPlugin = require('./index')

export interface TrackVitePluginOptions {
  /** 透传给 babel 插件的选项(moduleSource 等) */
  moduleSource?: string
}

/** Vite 插件最小结构化类型(避免引入 vite 依赖) */
export interface AquarushVitePlugin {
  name: string
  enforce: 'pre'
  transform(code: string, id: string): { code: string; map: null } | null
}

export function aquarushTrackVitePlugin(
  options: TrackVitePluginOptions = {},
): AquarushVitePlugin {
  return {
    name: 'aquarush-track',
    enforce: 'pre',
    transform(code: string, id: string) {
      if (!/\.[jt]sx?$/.test(id)) return null
      if (id.includes('node_modules')) return null
      // 快速路径:没有 track-* 的文件零开销
      if (!code.includes('track')) return null
      const result = transformSync(code, {
        filename: id,
        babelrc: false,
        configFile: false,
        parserOpts: { plugins: ['jsx', 'typescript'] },
        plugins: [[trackPlugin, options]],
        sourceMaps: false,
      })
      return { code: result?.code ?? '', map: null }
    },
  }
}

export default aquarushTrackVitePlugin
