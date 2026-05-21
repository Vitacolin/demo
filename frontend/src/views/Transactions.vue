<template>
  <div class="page-container">
    <header class="page-header">
      <h2 class="page-title">账单图谱 <span class="highlight">/ Ledger</span></h2>
      <div class="header-actions">
        <n-button ghost @click="exportCSV">导出 CSV</n-button>
        <n-button type="primary" @click="showAddModal = true">+ 手动记账</n-button>
      </div>
    </header>

    <!-- 过滤器与账本选择 -->
    <div class="filter-bar aesthetic-card">
      <div class="ledger-tabs">
        <span 
          v-for="tab in ledgerTabs" 
          :key="tab.value" 
          :class="['tab', { active: currentLedger === tab.value }]"
          @click="changeLedger(tab.value)"
        >
          {{ tab.label }}
        </span>
      </div>
      <div class="filter-inputs">
        <n-input v-model:value="searchQuery" placeholder="搜索账单、备注..." style="width: 200px;" clearable />
        <n-button ghost @click="fetchTransactions" :loading="isLoading">刷新数据 🔄</n-button>
      </div>
    </div>

    <!-- 极客风账单列表 -->
    <div class="transaction-list">
      <div v-if="filteredTransactions.length === 0 && !isLoading" class="empty-state">
        目前还没有账单记录，去“智能录入”跟 AI 聊两句吧！
      </div>
      
      <!-- 简单按日期分组展示的逻辑 -->
      <div v-for="tx in filteredTransactions" :key="tx.id" class="tx-item aesthetic-card">
        <div :class="['tx-icon', tx.type === 'expense' ? 'tx-out' : 'tx-in']">
          {{ tx.type === 'expense' ? '💸' : '💰' }}
        </div>
        <div class="tx-info">
          <div class="tx-title">{{ tx.description }}</div>
          <div class="tx-tags">
            <span class="tag ledger-tag">{{ tx.ledger || '日常账本' }}</span>
            <span class="tag">{{ tx.category }}</span>
            <span class="tag">{{ new Date(tx.date).toLocaleDateString() }}</span>
          </div>
        </div>
        <div :class="['tx-amount', tx.type === 'expense' ? 'out' : 'in']">
          {{ tx.type === 'expense' ? '-' : '+' }} ¥ {{ tx.amount.toFixed(2) }}
        </div>
        <n-button text type="error" @click="deleteTx(tx.id)" style="margin-left: 16px;">删除</n-button>
      </div>
    </div>

    <!-- 手动记账弹窗 -->
    <n-modal v-model:show="showAddModal" preset="card" title="手动记账" class="aesthetic-modal" style="width: 400px">
      <n-form :model="newTx" @submit.prevent="handleAddSubmit">
        <n-form-item label="描述" path="description">
          <n-input v-model:value="newTx.description" placeholder="如：打车、晚餐" />
        </n-form-item>
        
        <div style="display: flex; gap: 16px;">
          <n-form-item label="金额" path="amount" style="flex: 1">
            <n-input-number v-model:value="newTx.amount" placeholder="0.00" :min="0" :precision="2" style="width: 100%" />
          </n-form-item>
          <n-form-item label="类型" path="type" style="flex: 1">
            <n-select v-model:value="newTx.type" :options="[{label: '支出', value: 'expense'}, {label: '收入', value: 'income'}]" />
          </n-form-item>
        </div>

        <div style="display: flex; gap: 16px;">
          <n-form-item label="分类" path="category" style="flex: 1">
            <n-input v-model:value="newTx.category" placeholder="如：餐饮、交通" />
          </n-form-item>
          <n-form-item label="账本" path="ledger" style="flex: 1">
            <n-select v-model:value="newTx.ledger" :options="ledgerTabs" />
          </n-form-item>
        </div>

        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
          <n-button @click="showAddModal = false" quaternary>取消</n-button>
          <n-button type="primary" @click="handleAddSubmit" :loading="isAdding">保存</n-button>
        </div>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { NButton, NInput, NModal, NForm, NFormItem, NInputNumber, NSelect, useMessage } from 'naive-ui'
import axios from 'axios'

const transactions = ref<any[]>([])
const isLoading = ref(false)
const message = useMessage()
const API_BASE_URL = 'http://localhost:8000'

const currentLedger = ref('all')
const ledgerTabs = [
  { label: '全部账单', value: 'all' },
  { label: '日常账本', value: '日常账本' },
  { label: '✈️ 旅行账本', value: '旅行账本' },
  { label: '🏠 装修账本', value: '装修账本' }
]

const searchQuery = ref('')
const filteredTransactions = computed(() => {
  if (!searchQuery.value) return transactions.value
  const query = searchQuery.value.toLowerCase()
  return transactions.value.filter(tx => 
    tx.description.toLowerCase().includes(query) || 
    tx.category.toLowerCase().includes(query)
  )
})

const showAddModal = ref(false)
const isAdding = ref(false)
const newTx = ref({
  description: '',
  amount: null as number | null,
  type: 'expense',
  category: '其他',
  ledger: '日常账本'
})

const changeLedger = (ledgerValue: string) => {
  currentLedger.value = ledgerValue
  fetchTransactions()
}

const fetchTransactions = async () => {
  isLoading.value = true
  try {
    const res = await axios.get(`${API_BASE_URL}/api/transactions`, {
      params: { ledger: currentLedger.value, limit: 200 }
    })
    transactions.value = res.data
  } catch (error) {
    console.error(error)
    message.error('获取账单失败')
  } finally {
    isLoading.value = false
  }
}

const handleAddSubmit = async () => {
  if (!newTx.value.description || !newTx.value.amount) {
    message.warning('请填写描述和金额')
    return
  }
  isAdding.value = true
  try {
    await axios.post(`${API_BASE_URL}/api/transactions`, {
      description: newTx.value.description,
      amount: newTx.value.amount,
      type: newTx.value.type,
      category: newTx.value.category,
      ledger: newTx.value.ledger
    })
    message.success('记账成功')
    showAddModal.value = false
    // reset
    newTx.value = { description: '', amount: null, type: 'expense', category: '其他', ledger: '日常账本' }
    fetchTransactions()
  } catch (error) {
    message.error('记账失败')
  } finally {
    isAdding.value = false
  }
}

const deleteTx = async (id: number) => {
  try {
    await axios.delete(`${API_BASE_URL}/api/transactions/${id}`)
    message.success('删除成功')
    fetchTransactions()
  } catch (error) {
    message.error('删除失败')
  }
}

const exportCSV = () => {
  if (filteredTransactions.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  
  // CSV Header
  let csvContent = 'ID,日期,账本,类型,分类,描述,金额\n'
  
  // CSV Rows
  filteredTransactions.value.forEach(tx => {
    const date = new Date(tx.date).toLocaleString()
    const typeStr = tx.type === 'expense' ? '支出' : '收入'
    const amountStr = tx.type === 'expense' ? `-${tx.amount}` : `+${tx.amount}`
    
    // 处理可能包含逗号的字符串
    const desc = `"${tx.description.replace(/"/g, '""')}"`
    const ledger = `"${tx.ledger || '日常账本'}"`
    
    csvContent += `${tx.id},${date},${ledger},${typeStr},${tx.category},${desc},${amountStr}\n`
  })
  
  // Create Blob and Download
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' }) // \uFEFF for Excel UTF-8 BOM
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `账单导出_${new Date().toISOString().slice(0, 10)}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

onMounted(() => {
  fetchTransactions()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-title { font-size: 2rem; margin: 0; font-weight: 800; }
.header-actions { display: flex; gap: 16px; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; }
.ledger-tabs { display: flex; gap: 16px; }
.tab { padding: 8px 16px; border-radius: 8px; color: var(--text-muted); cursor: pointer; transition: all var(--transition-fast); }
.tab:hover { background: rgba(255,255,255,0.05); }
.tab.active { background: rgba(255,107,53,0.1); color: var(--accent-orange); font-weight: bold; }
.filter-inputs { display: flex; gap: 12px; }
.transaction-list { display: flex; flex-direction: column; gap: 16px; }
.empty-state { text-align: center; color: var(--text-muted); padding: 40px; border: 1px dashed rgba(255,255,255,0.1); border-radius: 12px; }
.tx-item { display: flex; align-items: center; padding: 16px 24px; cursor: pointer; margin-bottom: 0; }
.tx-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin-right: 20px; }
.tx-out { background: rgba(248,113,113,0.1); }
.tx-in { background: rgba(74,222,128,0.1); }
.tx-info { flex: 1; }
.tx-title { font-size: 1.1rem; font-weight: bold; margin-bottom: 6px; }
.tx-tags { display: flex; gap: 8px; }
.tag { font-size: 0.75rem; padding: 2px 8px; background: rgba(255,255,255,0.05); border-radius: 4px; color: var(--text-muted); }
.ledger-tag { color: var(--accent-orange); border: 1px solid rgba(255,107,53,0.3); }
.tx-amount { font-size: 1.4rem; font-weight: 800; font-family: 'Fira Code', monospace; }
.tx-amount.out { color: var(--text-main); }
.tx-amount.in { color: var(--success-color); }
</style>
