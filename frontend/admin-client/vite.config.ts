import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { aquarushStylexVitePlugin } from 'vite-plugin-aquarush-stylex'
import path from 'path'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [
    // StyleX 编译期抽取:须在 react() 之前 transform(dev 走运行时注入便于调试)
    aquarushStylexVitePlugin({ dev: command === 'serve' }),
    react({ babel: { plugins: [['babel-plugin-aquarush-track', {}]] } }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      // 代理 GraphQL 请求到后端
      '/graphql': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false,
      },
      // 保留现有的 API 代理（如果需要的话）
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false,
      },
      '/static': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false,
      },
    },
  },
}))
