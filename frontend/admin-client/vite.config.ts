import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
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
    },
  },
})
