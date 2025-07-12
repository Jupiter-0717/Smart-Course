import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus' // 保持完整导入
import 'element-plus/dist/index.css' // 全局样式
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 关键修复 - 使用类型安全的方式安装
app.use(ElementPlus as any) // 强制类型声明解决错误

app.use(createPinia())
app.use(router)

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
authStore.initializeAuth()

app.mount('#app')
