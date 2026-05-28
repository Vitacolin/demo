<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <h2 class="page-title">家庭账本 <span class="highlight">/ Family</span></h2>
        <p class="page-subtitle">管理您的家庭账本，邀请家庭成员一起记账</p>
      </div>
      <n-button type="primary" @click="showCreateModal = true">+ 创建家庭账本</n-button>
    </header>

    <!-- 家庭账本列表 -->
    <div class="families-grid" v-if="families.length > 0">
      <div v-for="family in families" :key="family.id" class="family-card aesthetic-card">
        <div class="family-header">
          <div class="family-icon">👨‍👩‍👧‍👦</div>
          <div class="family-info">
            <h3 class="family-name">{{ family.name }}</h3>
            <span class="family-meta">创建于 {{ formatDate(family.created_at) }}</span>
          </div>
        </div>
        <div class="family-stats">
          <div class="stat-item">
            <span class="stat-value">{{ family.members.length }}</span>
            <span class="stat-label">成员</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ family.transaction_count || 0 }}</span>
            <span class="stat-label">账单</span>
          </div>
        </div>
        <div class="family-members">
          <div class="members-title">家庭成员</div>
          <div class="members-list">
            <div v-for="member in family.members" :key="member.user_id" class="member-item">
              <span class="member-name">{{ member.username }}</span>
              <n-tag size="small" :type="getRoleTagType(member.role)">
                {{ getRoleLabel(member.role) }}
              </n-tag>
            </div>
          </div>
        </div>
        <div class="family-actions">
          <n-button ghost size="small" @click="selectFamily(family)">查看账本</n-button>
          <n-button ghost size="small" @click="showAddMemberModal(family)">邀请成员</n-button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">🏠</div>
      <h3>还没有家庭账本</h3>
      <p>创建一个家庭账本，邀请家人一起管理财务</p>
      <n-button type="primary" @click="showCreateModal = true">创建家庭账本</n-button>
    </div>

    <!-- 创建家庭弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="card" title="创建家庭账本" style="width: 400px">
      <n-form :model="createForm" @submit.prevent="handleCreate">
        <n-form-item label="账本名称" path="name">
          <n-input v-model:value="createForm.name" placeholder="如：我们的小家" />
        </n-form-item>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" @click="handleCreate" :loading="isCreating">创建</n-button>
        </div>
      </n-form>
    </n-modal>

    <!-- 添加成员弹窗 -->
    <n-modal v-model:show="showMemberModal" preset="card" title="邀请家庭成员" style="width: 400px">
      <n-form :model="memberForm" @submit.prevent="handleAddMember">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="memberForm.username" placeholder="请输入用户的用户名" />
        </n-form-item>
        <n-form-item label="角色" path="role">
          <n-select v-model:value="memberForm.role" :options="roleOptions" />
        </n-form-item>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
          <n-button @click="showMemberModal = false">取消</n-button>
          <n-button type="primary" @click="handleAddMember" :loading="isAddingMember">邀请</n-button>
        </div>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NButton, NModal, NForm, NFormItem, NInput, NSelect, NTag, useMessage } from 'naive-ui'
import api from '../api'
import { useRouter } from 'vue-router'

const router = useRouter()
const message = useMessage()

const families = ref<any[]>([])
const showCreateModal = ref(false)
const showMemberModal = ref(false)
const isCreating = ref(false)
const isAddingMember = ref(false)
const currentFamily = ref<any>(null)

const createForm = ref({
  name: ''
})

const memberForm = ref({
  username: '',
  role: 'MEMBER'
})

const roleOptions = [
  { label: '家主 (OWNER)', value: 'OWNER' },
  { label: '管理员 (ADMIN)', value: 'ADMIN' },
  { label: '普通成员 (MEMBER)', value: 'MEMBER' },
  { label: '访客 (VIEWER)', value: 'VIEWER' }
]

const getRoleLabel = (role: string) => {
  const labels: Record<string, string> = {
    'OWNER': '家主',
    'ADMIN': '管理员',
    'MEMBER': '成员',
    'VIEWER': '访客'
  }
  return labels[role] || role
}

const getRoleTagType = (role: string) => {
  const types: Record<string, any> = {
    'OWNER': 'error',
    'ADMIN': 'warning',
    'MEMBER': 'info',
    'VIEWER': 'default'
  }
  return types[role] || 'default'
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const fetchFamilies = async () => {
  try {
    const res = await api.get('/families')
    families.value = res.data
  } catch (error) {
    console.error('获取家庭列表失败', error)
    message.error('获取家庭列表失败')
  }
}

const handleCreate = async () => {
  if (!createForm.value.name.trim()) {
    message.warning('请输入家庭账本名称')
    return
  }
  isCreating.value = true
  try {
    await api.post('/families', createForm.value)
    message.success('创建成功')
    showCreateModal.value = false
    createForm.value = { name: '' }
    fetchFamilies()
  } catch (error: any) {
    message.error(error.response?.data?.detail || '创建失败')
  } finally {
    isCreating.value = false
  }
}

const showAddMemberModal = (family: any) => {
  currentFamily.value = family
  memberForm.value = { username: '', role: 'MEMBER' }
  showMemberModal.value = true
}

const handleAddMember = async () => {
  if (!memberForm.value.username.trim()) {
    message.warning('请输入用户名')
    return
  }
  isAddingMember.value = true
  try {
    await api.post(`/families/${currentFamily.value.id}/members`, memberForm.value)
    message.success('邀请成功')
    showMemberModal.value = false
    fetchFamilies()
  } catch (error: any) {
    message.error(error.response?.data?.detail || '邀请失败')
  } finally {
    isAddingMember.value = false
  }
}

const selectFamily = (family: any) => {
  // 保存当前选中的家庭到本地存储
  localStorage.setItem('currentFamily', JSON.stringify(family))
  message.success(`已切换到 ${family.name}`)
  // 跳转到账单图谱页面查看该家庭的账本
  router.push('/transactions')
}

onMounted(() => {
  fetchFamilies()
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 2rem;
  margin: 0;
  font-weight: 800;
}

.page-subtitle {
  color: var(--text-muted);
  margin-top: 8px;
}

.families-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 24px;
}

.family-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.family-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.family-icon {
  font-size: 2.5rem;
}

.family-name {
  margin: 0;
  font-size: 1.3rem;
  font-weight: bold;
}

.family-meta {
  color: var(--text-muted);
  font-size: 0.85rem;
}

.family-stats {
  display: flex;
  gap: 32px;
  padding: 16px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: var(--accent-orange);
}

.stat-label {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.family-members {
  flex: 1;
}

.members-title {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 6px;
}

.family-actions {
  display: flex;
  gap: 12px;
  margin-top: auto;
}

.empty-state {
  text-align: center;
  padding: 80px 40px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 24px;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  font-size: 1.3rem;
}

.empty-state p {
  color: var(--text-muted);
  margin: 0 0 24px 0;
}
</style>
