<template>
  <div class="page-container">
    <header class="page-header">
      <h2 class="page-title">系统管理 <span class="highlight">/ System</span></h2>
    </header>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
          <div class="stat-label">总用户数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📣</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.activeNotices || 0 }}</div>
          <div class="stat-label">系统公告</div>
        </div>
      </div>
    </div>

    <div class="tabs">
      <button v-for="tab in tabs" :key="tab.key" :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key">
        {{ tab.label }}
      </button>
    </div>

    <div class="tab-content">
      <!-- 用户管理 -->
      <div v-if="activeTab === 'users'" class="section">
        <div class="section-header">
          <h3>用户管理</h3>
        </div>
        <div class="data-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>角色</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.username }}</td>
                <td>
                  <span :class="['role-tag', user.role.toLowerCase()]">{{ user.role }}</span>
                </td>
                <td>{{ formatDate(user.createdAt) }}</td>
                <td>
                  <button v-if="user.role === 'ADMIN'" class="action-btn secondary"
                    @click="updateUserRole(user.id, 'USER')">
                    设为普通用户
                  </button>
                  <button v-else class="action-btn primary" @click="updateUserRole(user.id, 'ADMIN')">
                    设为管理员
                  </button>
                  <button class="action-btn danger" @click="deleteUser(user.id)">
                    删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 系统公告 -->
      <div v-if="activeTab === 'notices'" class="section">
        <div class="section-header">
          <h3>系统公告</h3>
          <button class="n-button n-button--primary-type" @click="showAddNotice = true">
            + 添加公告
          </button>
        </div>
        <div class="notice-list">
          <div v-for="notice in notices" :key="notice.id" :class="['notice-card', { inactive: !notice.isActive }]">
            <div class="notice-header">
              <h4>{{ notice.title }}</h4>
              <span :class="['status-badge', notice.isActive ? 'active' : 'inactive']">
                {{ notice.isActive ? '发布中' : '已关闭' }}
              </span>
            </div>
            <p class="notice-content">{{ notice.content }}</p>
            <div class="notice-footer">
              <span class="notice-date">{{ formatDate(notice.createdAt) }}</span>
              <div class="notice-actions">
                <button v-if="notice.isActive" class="action-btn secondary"
                  @click="toggleNoticeStatus(notice.id, false)">
                  关闭
                </button>
                <button v-else class="action-btn primary" @click="toggleNoticeStatus(notice.id, true)">
                  发布
                </button>
                <button class="action-btn danger" @click="deleteNotice(notice.id)">
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加公告弹窗 -->
    <n-modal v-model:show="showAddNotice" title="添加系统公告">
      <div class="modal-content">
        <n-form-item label="标题">
          <n-input v-model:value="newNotice.title" placeholder="请输入公告标题" />
        </n-form-item>
        <n-form-item label="内容">
          <n-input v-model:value="newNotice.content" type="textarea" :rows="4" placeholder="请输入公告内容" />
        </n-form-item>
        <div class="modal-actions">
          <n-button @click="showAddNotice = false">取消</n-button>
          <n-button type="primary" @click="addNotice">发布</n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { NModal, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import api from '../api'

const message = useMessage()

const tabs = [
  { key: 'users', label: '用户管理' },
  { key: 'notices', label: '系统公告' }
]

const activeTab = ref('users')
const users = ref<any[]>([])
const notices = ref<any[]>([])
const stats = ref({
  totalUsers: 0,
  activeNotices: 0
})

const showAddNotice = ref(false)
const newNotice = reactive({
  title: '',
  content: ''
})

const fetchStats = async () => {
  try {
    const res = await api.get('/system/stats')
    stats.value = res.data
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

const fetchUsers = async () => {
  try {
    const res = await api.get('/system/users')
    users.value = res.data
  } catch (error) {
    console.error('获取用户列表失败', error)
  }
}

const fetchNotices = async () => {
  try {
    const res = await api.get('/system/notices/all')
    notices.value = res.data
  } catch (error) {
    console.error('获取公告列表失败', error)
  }
}

const updateUserRole = async (userId: number, role: string) => {
  try {
    await api.patch(`/system/users/${userId}/role`, { role })
    message.success(`已将用户设为${role === 'ADMIN' ? '管理员' : '普通用户'}`)
    fetchUsers()
  } catch (error) {
    message.error('操作失败')
  }
}

const deleteUser = async (userId: number) => {
  if (!confirm('确定要删除该用户吗？')) return
  try {
    await api.delete(`/system/users/${userId}`)
    message.success('删除成功')
    fetchUsers()
    fetchStats()
  } catch (error) {
    message.error('删除失败')
  }
}

const addNotice = async () => {
  if (!newNotice.title || !newNotice.content) {
    message.warning('请填写标题和内容')
    return
  }
  try {
    await api.post('/system/notices', {
      title: newNotice.title,
      content: newNotice.content
    })
    message.success('发布成功')
    showAddNotice.value = false
    newNotice.title = ''
    newNotice.content = ''
    fetchNotices()
    fetchStats()
  } catch (error) {
    message.error('发布失败')
  }
}

const toggleNoticeStatus = async (noticeId: number, isActive: boolean) => {
  try {
    await api.patch(`/system/notices/${noticeId}`, { isActive })
    message.success(isActive ? '已发布' : '已关闭')
    fetchNotices()
    fetchStats()
  } catch (error) {
    message.error('操作失败')
  }
}

const deleteNotice = async (noticeId: number) => {
  if (!confirm('确定要删除该公告吗？')) return
  try {
    await api.delete(`/system/notices/${noticeId}`)
    message.success('删除成功')
    fetchNotices()
    fetchStats()
  } catch (error) {
    message.error('删除失败')
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  fetchStats()
  fetchUsers()
  fetchNotices()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 1.5rem;
  margin: 0;
}

.stats-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 200px;
}

.stat-icon {
  font-size: 2.5rem;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.8rem;
  font-weight: bold;
  color: var(--accent-orange);
}

.stat-label {
  font-size: 0.9rem;
  color: var(--text-muted);
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  background: var(--bg-card);
  padding: 8px;
  border-radius: 12px;
}

.tab-btn {
  padding: 10px 24px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: rgba(255, 107, 53, 0.1);
  color: var(--text-main);
}

.tab-btn.active {
  background: var(--accent-orange);
  color: #000;
}

.tab-content {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  font-size: 1.2rem;
}

.data-table {
  overflow-x: auto;
}

.data-table table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.data-table th {
  background: rgba(0, 0, 0, 0.1);
  font-weight: 600;
  color: var(--text-muted);
}

.data-table tr:hover {
  background: rgba(255, 107, 53, 0.05);
}

.role-tag {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: bold;
}

.role-tag.admin {
  background: rgba(255, 107, 53, 0.2);
  color: #ff6b35;
}

.role-tag.user {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  margin-right: 8px;
  transition: opacity 0.2s;
}

.action-btn:hover {
  opacity: 0.8;
}

.action-btn.primary {
  background: var(--accent-orange);
  color: #000;
}

.action-btn.secondary {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-main);
}

.action-btn.danger {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notice-card {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  padding: 20px;
  border-left: 4px solid var(--accent-orange);
}

.notice-card.inactive {
  opacity: 0.6;
  border-left-color: var(--text-muted);
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.notice-header h4 {
  margin: 0;
  font-size: 1.1rem;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: bold;
}

.status-badge.active {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.status-badge.inactive {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.notice-content {
  margin: 0 0 12px 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.notice-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice-date {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.modal-content {
  padding: 20px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
</style>