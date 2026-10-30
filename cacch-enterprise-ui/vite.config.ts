import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端构建与开发服务器配置
// 本地开发：/api/v1/keruyun 代理到集成平台（客如云订单），其余 /api 代理到后端 8080；
// 生产：pnpm build 后由 Nginx 托管 dist/ 并反向代理 /api/**
export default defineConfig({
  plugins: [vue()],
  build: {
    // 分包：原先主 chunk 达 1.12MB（gzip 371KB），触发 Vite 500KB 警告且拖慢首屏。
    // 按依赖来源拆分后可并行加载、充分利用浏览器缓存（业务代码变更不会让大依赖失效）
    // Element Plus 全量引入后本身体积即约 800KB（gzip 约 258KB），属依赖固有体积，
    // 已单独成块可被浏览器长期缓存，故将告警阈值上调，避免每次构建刷无意义警告
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (!id.includes('node_modules')) return
          // Element Plus 图标库独立，避免与主组件库互相牵连缓存
          if (id.includes('@element-plus/icons-vue')) return 'vendor-icons'
          // Element Plus 体积最大，单独成块
          if (id.includes('element-plus')) return 'vendor-element'
          // xlsx 及其底层依赖（客如云订单导出用），按需场景不必首屏加载
          if (id.includes('xlsx') || id.includes('adler-32') || id.includes('crc-32') || id.includes('cfb')) {
            return 'vendor-xlsx'
          }
          // Vue 生态核心
          if (id.includes('/vue/') || id.includes('vue-router') || id.includes('pinia') || id.includes('@vue')) {
            return 'vendor-vue'
          }
          if (id.includes('axios')) return 'vendor-axios'
          return 'vendor'
        }
      }
    }
  },
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
