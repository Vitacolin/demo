import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import axios from 'axios'

// 引入 vfonts (提供高级感字体)
import 'vfonts/Lato.css'
import 'vfonts/FiraCode.css'

const pinia = createPinia()
const app = createApp(App)

// 配置 Axios 拦截器
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

axios.interceptors.response.use(response => response, error => {
  if (error.response && error.response.status === 401) {
    localStorage.removeItem('token')
    if (router.currentRoute.value.path !== '/login') {
      router.push('/login')
    }
  }
  return Promise.reject(error)
})

app.use(pinia)
app.use(router)
app.mount('#app')
