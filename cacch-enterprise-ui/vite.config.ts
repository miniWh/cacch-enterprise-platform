import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端构建与开发服务器配置
// 本地开发：/api 代理到后端 8080；生产：pnpm build 后由 Nginx 托管 dist/ 并反向代理 /api/**
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
