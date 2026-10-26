import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

// 应用入口：路由（src/router）、Element Plus、全局样式后续接入
const app = createApp(App)
app.use(createPinia())
app.mount('#app')
