<template>
  <div class="auth-container">
    <div class="auth-card aesthetic-card">
      <div class="brand">
        <h1>LLM<span class="highlight">.</span></h1>
        <span class="brand-sub">Ledger Pro</span>
      </div>
      <h2 class="auth-title">登录系统</h2>
      
      <n-form :model="form" @submit.prevent="handleLogin" class="auth-form">
        <n-form-item label="用户名">
          <n-input v-model:value="form.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item label="密码">
          <n-input v-model:value="form.password" type="password" show-password-on="click" placeholder="请输入密码" />
        </n-form-item>
        
        <n-button type="primary" block @click="handleLogin" :loading="loading" class="submit-btn">登录</n-button>
      </n-form>
      
      <div class="auth-links">
        还没有账号？ <router-link to="/register" class="highlight-link">去注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()

const form = ref({
  username: '',
  password: ''
})
const loading = ref(false)

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    message.warning('请输入用户名和密码')
    return
  }
  
  loading.value = true
  try {
    const formData = new FormData()
    formData.append('username', form.value.username)
    formData.append('password', form.value.password)
    
    const res = await axios.post('http://localhost:8000/api/auth/login', formData)
    authStore.setToken(res.data.access_token)
    await authStore.fetchUser()
    
    message.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    message.error(error.response?.data?.detail || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-dark);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.brand {
  text-align: center;
  margin-bottom: 10px;
}

.brand h1 {
  font-size: 2.5rem;
  font-weight: 900;
  margin: 0;
  line-height: 1;
}

.brand-sub {
  color: var(--text-muted);
  font-size: 0.9rem;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.auth-title {
  text-align: center;
  margin: 0 0 20px 0;
  font-size: 1.5rem;
  color: var(--text-main);
}

.submit-btn {
  margin-top: 10px;
}

.auth-links {
  text-align: center;
  margin-top: 20px;
  font-size: 0.9rem;
  color: var(--text-muted);
}

.highlight-link {
  color: var(--accent-orange);
  text-decoration: none;
  font-weight: bold;
}

.highlight-link:hover {
  text-decoration: underline;
}
</style>
