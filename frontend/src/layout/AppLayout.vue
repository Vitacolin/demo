<template>
  <div class="app-layout">
    <!-- 顶部导航栏：悬浮毛玻璃效果 -->
    <header class="top-nav">
      <div class="nav-container">
        <div class="brand">
          <h1>LLM<span class="highlight">.</span></h1>
          <span class="brand-sub">Ledger Pro</span>
        </div>

        <nav class="nav-links">
          <router-link to="/dashboard" class="nav-item" active-class="active">
            <span class="nav-text">数字大屏</span>
          </router-link>
          <router-link to="/input" class="nav-item" active-class="active">
            <span class="nav-text">智能录入</span>
          </router-link>
          <router-link to="/transactions" class="nav-item" active-class="active">
            <span class="nav-text">账单图谱</span>
          </router-link>
          <router-link to="/families" class="nav-item" active-class="active">
            <span class="nav-text">家庭账本</span>
          </router-link>
          <router-link to="/subscriptions" class="nav-item" active-class="active">
            <span class="nav-text">周期账单</span>
          </router-link>
          <router-link to="/assets" class="nav-item" active-class="active">
            <span class="nav-text">资产分布</span>
          </router-link>
          <router-link to="/advisor" class="nav-item" active-class="active">
            <span class="nav-text">AI 顾问</span>
          </router-link>
          <router-link v-if="authStore.isAdmin" to="/system" class="nav-item admin-nav" active-class="active">
            <span class="nav-text">系统管理</span>
          </router-link>
        </nav>

        <div class="user-profile">
          <div class="user-info">
            <span class="name">{{ authStore.user?.username || '极客用户' }}</span>
            <span class="status">● 在线</span>
          </div>
          <n-dropdown :options="userOptions" @select="handleSelect">
            <n-avatar round size="medium"
              :src="authStore.user?.avatar_url || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix&backgroundColor=ff6b35'"
              style="cursor: pointer;" />
          </n-dropdown>
        </div>
      </div>
    </header>

    <!-- 主内容区：限制最大宽度并居中 -->
    <main class="main-content">
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <!-- 修改密码弹窗 -->
    <n-modal v-model:show="showPasswordModal" preset="card" title="修改密码" class="aesthetic-modal" style="width: 400px">
      <n-form :model="passwordForm" @submit.prevent="submitPassword">
        <n-form-item label="旧密码">
          <n-input v-model:value="passwordForm.old_password" type="password" show-password-on="click" />
        </n-form-item>
        <n-form-item label="新密码">
          <n-input v-model:value="passwordForm.new_password" type="password" show-password-on="click" />
        </n-form-item>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <n-button @click="showPasswordModal = false">取消</n-button>
          <n-button type="primary" @click="submitPassword">确认修改</n-button>
        </div>
      </n-form>
    </n-modal>

    <!-- 修改头像弹窗 -->
    <n-modal v-model:show="showAvatarModal" preset="card" title="修改头像" class="aesthetic-modal" style="width: 400px">
      <div style="display: flex; flex-direction: column; align-items: center; gap: 20px;">
        <n-avatar round :size="100"
          :src="authStore.user?.avatar_url || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix&backgroundColor=ff6b35'" />
        <n-upload action="http://localhost:8080/api/auth/avatar" :headers="uploadHeaders" accept="image/*"
          :show-file-list="false" @finish="handleAvatarSuccess" @error="handleAvatarError">
          <n-button type="primary">点击上传新头像</n-button>
        </n-upload>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { NAvatar, NDropdown, NModal, NForm, NFormItem, NInput, NButton, NUpload, useMessage } from 'naive-ui'
import { useAuthStore } from '../store/auth'
import { useRouter } from 'vue-router'
import api from '../api'

const authStore = useAuthStore()
const router = useRouter()
const message = useMessage()

const showPasswordModal = ref(false)
const showAvatarModal = ref(false)

const passwordForm = ref({ old_password: '', new_password: '' })

// 计算属性，动态生成上传请求头
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.token}`
}))

const userOptions = [
  { label: '修改头像', key: 'avatar' },
  { label: '修改密码', key: 'password' },
  { label: '退出登录', key: 'logout' }
]

const handleSelect = (key: string | number) => {
  if (key === 'logout') {
    authStore.clearAuth()
    router.push('/login')
    message.success('已退出登录')
  } else if (key === 'password') {
    passwordForm.value = { old_password: '', new_password: '' }
    showPasswordModal.value = true
  } else if (key === 'avatar') {
    showAvatarModal.value = true
  }
}

const submitPassword = async () => {
  if (!passwordForm.value.old_password || !passwordForm.value.new_password) {
    message.warning('请填写完整')
    return
  }
  try {
    await api.put('/auth/password', passwordForm.value)
    message.success('密码修改成功，请重新登录')
    showPasswordModal.value = false
    authStore.clearAuth()
    router.push('/login')
  } catch (error: any) {
    message.error(error.response?.data?.detail || '密码修改失败')
  }
}

const handleAvatarSuccess = async (options: { event?: ProgressEvent }) => {
  try {
    const res = JSON.parse((options.event?.target as XMLHttpRequest).response)
    if (res.status === 'success') {
      message.success('头像上传成功')
      showAvatarModal.value = false
      await authStore.fetchUser()
    }
  } catch (error) {
    message.error('头像上传响应解析失败')
  }
}

const handleAvatarError = () => {
  message.error('头像上传失败')
}

onMounted(async () => {
  if (!authStore.user && authStore.token) {
    await authStore.fetchUser()
  }
})
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* 顶部导航栏样式 */
.top-nav {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(18, 18, 18, 0.7);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
}

.nav-container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 40px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 品牌 Logo */
.brand {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.brand h1 {
  font-size: 2rem;
  font-weight: 900;
  margin: 0;
  line-height: 1;
  letter-spacing: -1px;
}

.brand-sub {
  color: var(--text-muted);
  font-size: 0.85rem;
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* 中间导航链接 */
.nav-links {
  display: flex;
  gap: 8px;
  height: 100%;
}

.nav-item {
  text-decoration: none;
  color: var(--text-muted);
  padding: 0 24px;
  height: 100%;
  display: flex;
  align-items: center;
  transition: all var(--transition-fast);
  position: relative;
}

/* 底部发光横线 */
.nav-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 100%;
  height: 3px;
  background: var(--accent-orange);
  transition: transform var(--transition-fast);
  border-radius: 3px 3px 0 0;
  box-shadow: 0 -2px 8px rgba(255, 107, 53, 0.5);
}

.nav-item:hover {
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.02);
}

.nav-item.active {
  color: var(--accent-orange);
  font-weight: bold;
  background: linear-gradient(to top, rgba(255, 107, 53, 0.08) 0%, transparent 100%);
}

.nav-item.active::after {
  transform: translateX(-50%) scaleX(1);
}

/* 用户信息区 */
.user-profile {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user-info .name {
  font-weight: bold;
  font-size: 0.95rem;
}

.user-info .status {
  font-size: 0.75rem;
  color: var(--success-color);
  margin-top: 2px;
}

/* 主内容区 */
.main-content {
  flex: 1;
  padding: 40px 0;
}

.content-wrapper {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 40px;
}

/* 反主流：路由切换动画使用缩放+透明度+错放，而非平移 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: all var(--transition-slow);
}

.page-fade-enter-from {
  opacity: 0;
  transform: scale(0.98) translateY(10px);
  filter: blur(4px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: scale(1.02) translateY(-10px);
  filter: blur(4px);
}
</style>
