import type { UserConfigExport } from "@tarojs/cli";
export default {
  mini: {
    // Disable source maps in production to reduce bundle size
    sourceMap: false,
    // Enable compression for smaller bundle size
    compress: {
      enable: true,
      options: {
        // Enable gzip compression
        gzip: {
          enable: true,
          threshold: 10240
        }
      }
    },
    // Optimize chunks for better loading
    chunkSplitting: {
      rules: {
        // Split vendor code
        vendors: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          chunks: 'all',
          minSize: 30000,
          maxSize: 0
        }
      }
    }
  },
  h5: {
    /**
     * WebpackChain 插件配置
     * @docs https://github.com/neutrinojs/webpack-chain
     */
    webpackChain (chain) {
      /**
       * 如果 h5 端编译后体积过大，可以使用 webpack-bundle-analyzer 插件对打包体积进行分析。
       * @docs https://github.com/webpack-contrib/webpack-bundle-analyzer
       */
      chain.plugin('analyzer')
        .use(require('webpack-bundle-analyzer').BundleAnalyzerPlugin, [{
          analyzerMode: 'static',
          openAnalyzer: false,
          reportFilename: '../../bundle-analysis-report.html'
        }])
      /**
       * 如果 h5 端首屏加载时间过长，可以使用 prerender-spa-plugin 插件预加载首页。
       * @docs https://github.com/chrisvfritz/prerender-spa-plugin
       */
      // const path = require('path')
      // const Prerender = require('prerender-spa-plugin')
      // const staticDir = path.join(__dirname, '..', 'dist')
      // chain
      //   .plugin('prerender')
      //   .use(new Prerender({
      //     staticDir,
      //     routes: [ '/pages/index/index' ],
      //     postProcess: (context) => ({ ...context, outputPath: path.join(staticDir, 'index.html') })
      //   }))
    }
  }
} satisfies UserConfigExport<'vite'>
