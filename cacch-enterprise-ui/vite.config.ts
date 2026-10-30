import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端构建与开发服务器配置
// 本地开发：/api/v1/keruyun 代理到集成平台（客如云订单），其余 /api 代理到后端 8080；
// 生产：pnpm build 后由 Nginx 托管 dist/ 并反向代理 /api/**
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 客如云订单接口走集成平台（须放在 /api 之前以优先匹配）
      '/api/v1/keruyun': {
        target: 'http://10.80.68.10:8081',
        changeOrigin: true
      },
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
