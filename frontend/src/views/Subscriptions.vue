<template>
  <div class="subscriptions-page">
    <div class="page-header">
      <div class="title-section">
        <h2>周期性账单</h2>
        <p class="subtitle">管理你的订阅服务和固定支出</p>
      </div>
      <button class="add-btn" @click="showAddModal = true">
        <span class="icon">+</span>
        新增订阅
      </button>
    </div>

    <div class="subs-grid">
      <div v-for="sub in subscriptions" :key="sub.id" class="sub-card" :class="{ 'inactive': !sub.is_active }">
        <div class="card-header">
          <div class="sub-icon">{{ getCategoryIcon(sub.category) }}</div>
          <div class="sub-info">
            <h3>{{ sub.name }}</h3>
            <span class="category-tag">{{ sub.category }}</span>
          </div>
          <div class="status-toggle" @click="toggleStatus(sub.id)">
            <div class="toggle-track" :class="{ 'active': sub.is_active }">
              <div class="toggle-thumb"></div>
            </div>
          </div>
        </div>
        
        <div class="card-body">
          <div class="amount-display">
            <span class="currency">¥</span>
            <span class="amount">{{ sub.amount.toFixed(2) }}</span>
            <span class="cycle">/ {{ getCycleLabel(sub.cycle) }}</span>
          </div>
          
          <div class="billing-info">
            <div class="info-row">
              <span class="label">下次扣费日</span>
              <span class="value" :class="{ 'urgent': isUrgent(sub.next_billing_date) && sub.is_active }">
                {{ formatDate(sub.next_billing_date) }}
              </span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <button class="delete-btn" @click="deleteSub(sub.id)">删除</button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="subscriptions.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">🔄</div>
        <p>暂无周期性账单</p>
        <p class="empty-sub">点击右上角添加你的第一个订阅吧</p>
      </div>
    </div>

    <!-- 添加订阅弹窗 (使用 Naive UI) -->
    <n-modal v-model:show="showAddModal" preset="card" title="新增周期性账单" class="aesthetic-modal" style="width: 400px">
      <n-form :model="newSub" @submit.prevent="handleAddSubmit">
        <n-form-item label="账单名称" path="name">
          <n-input v-model:value="newSub.name" placeholder="如：Netflix, 房租, 健身房" />
        </n-form-item>
        
        <div class="form-row">
          <n-form-item label="金额" path="amount" style="flex: 1">
            <n-input-number v-model:value="newSub.amount" placeholder="0.00" :min="0" :precision="2" style="width: 100%" />
          </n-form-item>
          <n-form-item label="周期" path="cycle" style="flex: 1">
            <n-select v-model:value="newSub.cycle" :options="cycleOptions" :consistent-menu-width="false" />
          </n-form-item>
        </div>

        <n-form-item label="分类" path="category">
          <n-select v-model:value="newSub.category" :options="categoryOptions" :consistent-menu-width="false" />
        </n-form-item>

        <n-form-item label="下次扣费日" path="next_billing_date">
          <n-date-picker v-model:value="newSub.next_billing_date_ts" type="date" clearable style="width: 100%" />
        </n-form-item>

        <div class="modal-actions">
          <n-button @click="showAddModal = false" quaternary>取消</n-button>
          <n-button type="primary" @click="handleAddSubmit" color="#ff6b35">保存</n-button>
        </div>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NModal, NForm, NFormItem, NInput, NInputNumber, NSelect, NDatePicker, NButton } from 'naive-ui'
import axios from 'axios'

const API_BASE = 'http://127.0.0.1:8000/api'

interface Subscription {
  id: number
  name: string
  amount: number
  cycle: string
  next_billing_date: string
  category: string
  is_active: boolean
}

const subscriptions = ref<Subscription[]>([])
const loading = ref(false)
const showAddModal = ref(false)

const newSub = ref({
  name: '',
  amount: null as number | null,
  cycle: 'monthly',
  next_billing_date_ts: Date.now(),
  category: '娱乐'
})

const cycleOptions = [
  { label: '每月', value: 'monthly' },
  { label: '每年', value: 'yearly' },
  { label: '每周', value: 'weekly' }
]

const categoryOptions = [
  { label: '娱乐', value: '娱乐' },
  { label: '住房', value: '住房' },
  { label: '服务', value: '服务' },
  { label: '保险', value: '保险' },
  { label: '其他', value: '其他' }
]

const fetchSubscriptions = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${API_BASE}/subscriptions`)
    subscriptions.value = res.data
  } catch (error) {
    console.error("获取订阅失败:", error)
  } finally {
    loading.value = false
  }
}

const handleAddSubmit = async () => {
  try {
    const payload = {
      name: newSub.value.name,
      amount: newSub.value.amount || 0,
      cycle: newSub.value.cycle,
      category: newSub.value.category,
      next_billing_date: new Date(newSub.value.next_billing_date_ts).toISOString()
    }
    await axios.post(`${API_BASE}/subscriptions`, payload)
    showAddModal.value = false
    // Reset form
    newSub.value = {
      name: '',
      amount: null,
      cycle: 'monthly',
      next_billing_date_ts: Date.now(),
      category: '娱乐'
    }
    fetchSubscriptions()
  } catch (error) {
    console.error("添加失败:", error)
    alert("添加失败，请检查输入")
  }
}

const toggleStatus = async (id: number) => {
  try {
    await axios.put(`${API_BASE}/subscriptions/${id}/toggle`)
    fetchSubscriptions()
  } catch (error) {
    console.error("切换状态失败:", error)
  }
}

const deleteSub = async (id: number) => {
  if (!confirm("确定要删除这个周期性账单吗？")) return
  try {
    await axios.delete(`${API_BASE}/subscriptions/${id}`)
    fetchSubscriptions()
  } catch (error) {
    console.error("删除失败:", error)
  }
}

// 辅助工具函数
const getCategoryIcon = (cat: string) => {
  const icons: Record<string, string> = {
    '娱乐': '🎬', '住房': '🏠', '服务': '🔧', '保险': '🛡️', '其他': '📦'
  }
  return icons[cat] || '🏷️'
}

const getCycleLabel = (cycle: string) => {
  const labels: Record<string, string> = {
    'monthly': '月', 'yearly': '年', 'weekly': '周'
  }
  return labels[cycle] || cycle
}

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const isUrgent = (dateStr: string) => {
  const billingDate = new Date(dateStr)
  const today = new Date()
  const diffDays = Math.ceil((billingDate.getTime() - today.getTime()) / (1000 * 3600 * 24))
  return diffDays <= 3 && diffDays >= 0
}

onMounted(() => {
  fetchSubscriptions()
})
</script>

<style scoped>
.subscriptions-page {
  animation: fade-in var(--transition-slow) var(--bounce-curve);
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 40px;
}

.title-section h2 {
  font-size: 2.5rem;
  margin: 0 0 8px 0;
  color: var(--text-main);
  letter-spacing: -1px;
}

.subtitle {
  color: var(--text-muted);
  margin: 0;
  font-size: 1.1rem;
}

.add-btn {
  background: var(--accent-orange);
  color: #fff;
  border: none;
  padding: 12px 24px;
  border-radius: 12px;
  font-weight: bold;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all var(--transition-fast) var(--bounce-curve);
  box-shadow: 0 4px 15px rgba(255, 107, 53, 0.3);
}

.add-btn:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.4);
}

/* 栅格布局 */
.subs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

/* 卡片样式 */
.sub-card {
  background: var(--surface-light);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 24px;
  transition: all var(--transition-fast) var(--bounce-curve);
  position: relative;
  overflow: hidden;
}

.sub-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: var(--accent-orange);
  opacity: 0.8;
}

.sub-card:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.2);
  border-color: rgba(255, 107, 53, 0.3);
}

.sub-card.inactive {
  opacity: 0.6;
  filter: grayscale(0.5);
}
.sub-card.inactive::before {
  background: var(--text-muted);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.sub-icon {
  font-size: 2rem;
  background: rgba(255, 255, 255, 0.05);
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.sub-info {
  flex: 1;
}

.sub-info h3 {
  margin: 0 0 4px 0;
  font-size: 1.2rem;
  color: var(--text-main);
}

.category-tag {
  font-size: 0.75rem;
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-muted);
  padding: 2px 8px;
  border-radius: 4px;
}

/* Toggle 开关 */
.toggle-track {
  width: 44px;
  height: 24px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.toggle-track.active {
  background: var(--success-color);
}

.toggle-thumb {
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: transform var(--transition-fast) var(--bounce-curve);
}

.toggle-track.active .toggle-thumb {
  transform: translateX(20px);
}

.card-body {
  margin-bottom: 20px;
}

.amount-display {
  margin-bottom: 16px;
}

.currency {
  font-size: 1.2rem;
  color: var(--text-muted);
}

.amount {
  font-size: 2.5rem;
  font-weight: 900;
  color: var(--text-main);
  margin: 0 4px;
  letter-spacing: -1px;
}

.cycle {
  color: var(--text-muted);
  font-size: 0.9rem;
}

.billing-info {
  background: rgba(0, 0, 0, 0.2);
  padding: 12px;
  border-radius: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
}

.info-row .label {
  color: var(--text-muted);
}

.info-row .value {
  color: var(--text-main);
  font-weight: bold;
}

.info-row .value.urgent {
  color: var(--accent-orange);
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  padding-top: 16px;
}

.delete-btn {
  background: transparent;
  border: none;
  color: var(--danger-color);
  cursor: pointer;
  font-size: 0.9rem;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background var(--transition-fast);
}

.delete-btn:hover {
  background: rgba(248, 113, 113, 0.1);
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-sub {
  font-size: 0.9rem;
  opacity: 0.7;
}

.form-row {
  display: flex;
  gap: 16px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
}
</style>
