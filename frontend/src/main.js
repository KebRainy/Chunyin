import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './styles/global.css'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
// 全局错误处理，避免错误消息显示为"????"
app.config.errorHandler = (err, instance, info) => {
  // 静默处理错误，不显示"????"
  // 错误信息已经在各个组件的catch中处理
}

// 捕获未处理的Promise错误
window.addEventListener('unhandledrejection', (event) => {
  // 静默处理，避免显示"????"
  event.preventDefault()
})
app.use(router)
app.use(pinia)
app.use(ElementPlus)
app.mount('#app')
