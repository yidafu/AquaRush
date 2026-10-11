import { defineConfig, type UserConfigExport } from '@tarojs/cli'
import devConfig from './dev'
import prodConfig from './prod'
import path from 'node:path'
import { aquarushTrackVitePlugin } from 'babel-plugin-aquarush-track/vite'
import { aquarushStylexVitePlugin } from 'vite-plugin-aquarush-stylex'

process.env.BROWSERSLIST_ENV = process.env.NODE_ENV

// weapp-stylex 只支持小程序端(TARO_ENV=weapp 硬校验);h5 构建改走自研
// vite 抽取插件(convertRpx: rpx→px÷2,750 设计稿基准→375 web 基准),
// 与 admin 端同一条 virtual:stylex.css 管线。非 h5 时把虚拟模块别名到
// 空样式文件,保证 app.tsx 的静态 import 在 weapp 构建下同样可解析。
const isH5 = process.env.TARO_ENV === 'h5'

// Admin build configuration

// https://taro-docs.jd.com/docs/next/config#defineconfig-辅助函数
export default defineConfig<'vite'>(async (merge, { command, mode }) => {
  const baseConfig: UserConfigExport<'vite'> = {
    projectName: 'user-miniapp',
    date: '2025-12-2',
    env: {
      APP_ID: process.env.TARO_APP_ID || '',
      API_BASE_URL: process.env.TARO_APP_API_BASE_URL ?? '',
    },
    designWidth: 750,
    deviceRatio: {
      640: 2.34 / 2,
      750: 1,
      375: 2,
      828: 1.81 / 2
    },
    sourceRoot: 'src',
    outputRoot: 'dist',
    // StyleX 编译期抽取(weapp-stylex:小程序无 DOM,不能运行时注入 <style>;
    // 自动生成并引入 stylex.wxss,在 Taro 尺寸转换之后输出,长度值显式写 rpx)。
    // importSources 桥接 @stylexjs/stylex:@aquarush/common 的 tokens 与两端共享。
    plugins: isH5
      ? []
      : [
          [
            require.resolve('weapp-stylex/taro'),
            { importSources: ['weapp-stylex', '@weapp-stylex/core', '@stylexjs/stylex'] },
          ],
        ],
    defineConstants: {
    },
    copy: {
      patterns: [
        { from: 'src/assets/icons/', to: 'dist/assets/icons/', ignore:['*.svg', '*.py']},
        { from: 'src/libs/', to: 'dist/libs/' }
      ],
      options: {
      }
    },
    alias: {
      '@': path.resolve(__dirname, '..', 'src'),
      ...(isH5
        ? {}
        : { 'virtual:stylex.css': path.resolve(__dirname, '..', 'src/styles/stylex-empty.css') }),
    },
    framework: 'react',
    compiler: {
      type: 'vite',
      vitePlugins: [
        // 修复 taro:vite-style 与 weapp-stylex WxssEmitter 的 key/fileName 错位:
        // taro style.js 只改 asset.fileName(.css→.wxss)不改 bundle key,而
        // weapp-stylex 的 companion 判定按 key 查 `bundle['pages/x/index.wxss']`
        // 恒为缺失,会给 app/每个页面发一个仅含 @import stylex.wxss 的同名资产,
        // 覆盖真样式内容(表现:app.wxss 25 字节、app-origin.wxss 无人引用、
        // 页面 scss 内容丢失)。此处在 normal 阶段(style.js 之后、emitter 之前)
        // 把 key 重排到与 fileName 一致,companion 判定即恢复正常。
        {
          name: 'aquarush-wxss-rekey',
          generateBundle(_o: unknown, bundle: Record<string, { type: string; fileName?: string }>) {
            for (const [key, asset] of Object.entries(bundle)) {
              const fileName = asset.fileName
              if (
                asset.type === 'asset' &&
                typeof fileName === 'string' &&
                fileName !== key &&
                /\.(?:css|wxss)$/.test(fileName)
              ) {
                bundle[fileName] = asset
                delete bundle[key]
              }
            }
          },
        },
        // 声明式埋点:taro vite 编译器不读 babel.config.js,由本插件在 transform 阶段
        // 把 track-* props 编译成 <Track> 包裹(见 babel-plugin-aquarush-track)
        aquarushTrackVitePlugin(),
        // h5:自研 StyleX 抽取(rpx→px÷2),补上 weapp-stylex 不支持的浏览器端
        ...(isH5 ? [aquarushStylexVitePlugin({ convertRpx: true })] : []),
      ]
    },
    mini: {
      postcss: {
        pxtransform: {
          enable: true,
          config: {

          }
        },
        cssModules: {
          enable: false, // 默认为 false，如需使用 css modules 功能，则设为 true
          config: {
            namingPattern: 'module', // 转换模式，取值为 global/module
            generateScopedName: '[name]__[local]___[hash:base64:5]'
          }
        }
      },
    },
    h5: {
      publicPath: process.env.PUBLIC_PATH || '/weapp/',
      staticDirectory: 'static',

      miniCssExtractPluginOption: {
        ignoreOrder: true,
        filename: 'css/[name].[hash].css',
        chunkFilename: 'css/[name].[chunkhash].css'
      },
      postcss: {
        autoprefixer: {
          enable: true,
          config: {}
        },
        cssModules: {
          enable: false, // 默认为 false，如需使用 css modules 功能，则设为 true
          config: {
            namingPattern: 'module', // 转换模式，取值为 global/module
            generateScopedName: '[name]__[local]___[hash:base64:5]'
          }
        }
      },
    },
    rn: {
      appName: 'taroDemo',
      postcss: {
        cssModules: {
          enable: false, // 默认为 false，如需使用 css modules 功能，则设为 true
        }
      }
    }
  }
  if (process.env.NODE_ENV === 'development') {
    // 本地开发构建配置（不混淆压缩）
    return merge({}, baseConfig, devConfig)
  }
  // 生产构建配置（默认开启压缩混淆等）
  return merge({}, baseConfig, prodConfig)
})
