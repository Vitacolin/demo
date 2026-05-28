<template>
  <div class="page-container">
    <header class="page-header">
      <h2 class="page-title">专属财务顾问 <span class="highlight">/ AI Advisor</span></h2>
      <div class="header-actions">
        <!-- 家庭账本选择 -->
        <div class="family-selector" v-if="families.length > 0" style="margin-right: 16px;">
          <span>账本范围：</span>
          <n-select v-model:value="selectedFamilyId" :options="familyOptions" placeholder="个人账本" style="width: 180px;"
            @update:value="onFamilyChange" />
        </div>
        <div class="date-range-picker">
          <label>分析时段：</label>
          <input type="date" v-model="startDate" class="date-input" />
          <span class="date-sep">—</span>
          <input type="date" v-model="endDate" class="date-input" />
          <button class="btn-quick" @click="setQuickRange('month')">本月</button>
          <button class="btn-quick" @click="setQuickRange('quarter')">近三月</button>
          <button class="btn-quick" @click="setQuickRange('year')">本年</button>
          <button class="btn-quick" @click="setQuickRange('all')">全部</button>
        </div>
      </div>
    </header>

    <div class="advisor-content">
      <!-- 财务诊断报告 -->
      <div class="report-card aesthetic-card" :class="{ 'loading-scan': loading }">
        <div class="report-header">
          <h3>财务诊断报告</h3>
          <div class="report-meta">
            <span class="scope-tag" v-if="selectedFamilyId">📦 {{ selectedFamilyName }}</span>
            <span class="period-tag" v-if="periodLabel">{{ periodLabel }}</span>
            <span class="date">{{ new Date().toLocaleString() }}</span>
          </div>
        </div>

        <div class="report-body">
          <div v-if="loading" class="loading-text">
            正在分析您的财务数据并生成报告... 请稍候...
          </div>
          <div v-else-if="report" class="report-content" v-html="renderMarkdown(report)"></div>
          <div v-else class="empty-state">
            选择时间段后点击"生成诊断报告"，AI 顾问将为您分析财务状况
          </div>
        </div>

        <div class="report-footer">
          <n-button type="primary" block @click="generateReport" :loading="loading">生成诊断报告</n-button>
        </div>
      </div>

      <!-- 交互问答区 -->
      <div class="qa-section aesthetic-card">
        <div class="qa-header">
          <h3>与顾问对话</h3>
          <span class="chat-hint" v-if="report">对报告某部分不满意？直接告诉顾问需要修改的地方</span>
        </div>
        <div class="qa-list" ref="qaListRef">
          <div v-for="(item, index) in chatHistory" :key="index" class="qa-item">
            <span class="q">Q: {{ item.q }}</span>
            <span class="a" v-html="renderMarkdown(item.a)"></span>
          </div>
        </div>
        <n-input v-model:value="chatInput" type="textarea" placeholder="对报告有修改意见？或提出理财问题..." :autosize="{ minRows: 2 }"
          @keydown.enter.prevent="sendChat" :disabled="chatLoading" />
        <n-button type="primary" style="margin-top: 12px;" @click="sendChat" :loading="chatLoading">
          发送问题
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { NButton, NInput, NSelect, useMessage } from 'naive-ui'
import api from '../api'
import { marked } from 'marked'

const persona = ref('roast')
const startDate = ref('')
const endDate = ref('')
const report = ref('')
const loading = ref(false)
const chatInput = ref('')
const chatLoading = ref(false)
const message = useMessage()
const qaListRef = ref<HTMLElement | null>(null)
const chatHistory = ref<{ q: string; a: string }[]>([])

// 家庭账本相关
const families = ref<any[]>([])
const selectedFamilyId = ref<number | null>(null)

const familyOptions = computed(() => {
  return [
    { label: '个人账本', value: null },
    ...families.value.map(f => ({ label: f.name, value: f.id }))
  ]
})

const selectedFamilyName = computed(() => {
  if (!selectedFamilyId.value) return ''
  const family = families.value.find(f => f.id === selectedFamilyId.value)
  return family?.name || ''
})

const fetchFamilies = async () => {
  try {
    const res = await api.get('/families')
    families.value = res.data
  } catch (error) {
    console.error('获取家庭列表失败', error)
  }
}

const onFamilyChange = () => {
  // 切换账本后清空报告
  report.value = ''
  chatHistory.value = []
}

onMounted(() => {
  setQuickRange('month')
  fetchFamilies()
})

const periodLabel = computed(() => {
  if (startDate.value && endDate.value) {
    return `${startDate.value} 至 ${endDate.value}`
  }
  return '全部时间'
})

function setQuickRange(type: string) {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()

  if (type === 'month') {
    startDate.value = formatDate(new Date(y, m, 1))
    endDate.value = formatDate(new Date(y, m + 1, 0))
  } else if (type === 'quarter') {
    startDate.value = formatDate(new Date(y, m - 2, 1))
    endDate.value = formatDate(new Date(y, m + 1, 0))
  } else if (type === 'year') {
    startDate.value = formatDate(new Date(y, 0, 1))
    endDate.value = formatDate(new Date(y, 11, 31))
  } else {
    startDate.value = ''
    endDate.value = ''
  }
}

function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function renderMarkdown(text: string): string {
  try {
    return marked(text)
  } catch {
    return text
  }
}

async function generateReport() {
  loading.value = true
  report.value = ''
  try {
    const params: Record<string, any> = { persona: persona.value }
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    if (selectedFamilyId.value) params.familyId = selectedFamilyId.value

    const res = await api.get('/advisor', {
      params,
      timeout: 120000
    })
    report.value = res.data.report
  } catch (e: any) {
    console.error('生成报告失败:', e)
    if (e.response && e.response.status === 401) {
      message.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else {
      message.error('生成报告失败，请检查后端和大模型是否正常运行')
    }
  } finally {
    loading.value = false
  }
}

async function sendChat() {
  const question = chatInput.value.trim()
  if (!question) return

  chatHistory.value.push({ q: question, a: '思考中...' })
  chatInput.value = ''
  chatLoading.value = true
  await scrollToBottom()

  try {
    const payload: Record<string, any> = {
      question,
      persona: persona.value,
      currentReport: report.value || ''
    }
    if (selectedFamilyId.value) {
      payload.familyId = selectedFamilyId.value
    }
    const res = await api.post('/advisor/chat', payload, {
      timeout: 120000
    })
    const answer = res.data.answer
    const isReportUpdate = res.data.isReportUpdate

    chatHistory.value[chatHistory.value.length - 1].a = answer

    if (isReportUpdate && report.value) {
      report.value = answer
    }
  } catch (e: any) {
    console.error('发送问题失败:', e)
    if (e.response && e.response.status === 401) {
      chatHistory.value[chatHistory.value.length - 1].a = '登录已过期，请重新登录'
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else {
      chatHistory.value[chatHistory.value.length - 1].a = '抱歉，请求失败，请检查后端和大模型是否正常运行。'
      message.error('请求失败')
    }
  } finally {
    chatLoading.value = false
    await scrollToBottom()
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (qaListRef.value) {
    qaListRef.value.scrollTop = qaListRef.value.scrollHeight
  }
}
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-size: 2rem;
  margin: 0;
  font-weight: 800;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.family-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.family-selector span {
  font-size: 14px;
  color: var(--text-muted);
}

.scope-tag {
  background: rgba(255, 107, 53, 0.2);
  color: #ff6b35;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.date-range-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.date-range-picker label {
  font-size: 14px;
  color: var(--text-muted);
}

.date-input {
  padding: 6px 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-main);
  outline: none;
  transition: border-color var(--transition-fast);
}

.date-input:focus {
  border-color: var(--accent-orange);
}

.date-sep {
  color: var(--text-muted);
  font-size: 14px;
}

.btn-quick {
  padding: 4px 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-muted);
  cursor: pointer;
  font-size: 13px;
  transition: all var(--transition-fast);
}

.btn-quick:hover {
  border-color: var(--accent-orange);
  color: var(--accent-orange);
  background: rgba(255, 107, 53, 0.08);
}

.advisor-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 30px;
}

/* 报告卡片 */
.report-card {
  position: relative;
  overflow: hidden;
  min-height: 400px;
}

.loading-scan::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--accent-orange), transparent);
  animation: scanline 2s linear infinite;
}

@keyframes scanline {
  0% {
    transform: translateX(-100%);
  }

  100% {
    transform: translateX(100%);
  }
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 30px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 16px;
}

.report-header h3 {
  margin: 0;
  font-size: 1.5rem;
  color: var(--text-main);
}

.report-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.period-tag {
  font-size: 0.85rem;
  color: var(--accent-orange);
  background: rgba(255, 107, 53, 0.15);
  padding: 2px 10px;
  border-radius: 12px;
}

.date {
  color: var(--text-muted);
  font-size: 0.85rem;
}

.loading-text {
  color: var(--accent-orange);
  font-style: italic;
  text-align: center;
  margin-top: 50px;
}

.empty-state {
  text-align: center;
  color: var(--text-muted);
  padding: 60px 24px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

.report-content {
  font-size: 1rem;
  color: var(--text-main);
  line-height: 1.7;
}

.report-content :deep(h1) {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--accent-secondary);
  margin: 24px 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--accent-orange);
}

.report-content :deep(h2) {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--accent-secondary);
  margin: 20px 0 12px 0;
  padding-left: 12px;
  border-left: 4px solid var(--accent-orange);
}

.report-content :deep(h3) {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-main);
  margin: 16px 0 8px 0;
}

.report-content :deep(p) {
  margin-bottom: 12px;
  text-align: justify;
}

.report-content :deep(strong) {
  color: var(--accent-orange);
  font-weight: 600;
}

.report-content :deep(ul),
.report-content :deep(ol) {
  padding-left: 24px;
  margin-bottom: 12px;
}

.report-content :deep(li) {
  margin-bottom: 6px;
  position: relative;
}

.report-content :deep(li::marker) {
  color: var(--accent-orange);
}

.report-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 8px;
  overflow: hidden;
}

.report-content :deep(th) {
  background: rgba(255, 107, 53, 0.15);
  color: var(--accent-orange);
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 0.9rem;
}

.report-content :deep(td) {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.report-content :deep(tr:hover) {
  background: rgba(255, 107, 53, 0.05);
}

.report-content :deep(.highlight-box) {
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.1), rgba(255, 193, 7, 0.1));
  border-left: 4px solid var(--accent-orange);
  padding: 16px;
  margin: 16px 0;
  border-radius: 0 8px 8px 0;
}

.report-content :deep(.summary-card) {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
  margin: 16px 0;
}

.report-content :deep(.summary-item) {
  background: rgba(255, 255, 255, 0.03);
  padding: 16px;
  border-radius: 8px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.report-content :deep(.summary-label) {
  display: block;
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.report-content :deep(.summary-value) {
  display: block;
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--accent-secondary);
}

.report-content :deep(.positive) {
  color: #10b981;
}

.report-content :deep(.negative) {
  color: #ef4444;
}

.report-content :deep(.warning) {
  background: rgba(251, 191, 36, 0.1);
  padding: 12px 16px;
  border-radius: 6px;
  margin: 12px 0;
  border-left: 3px solid #fbbf24;
}

.report-content :deep(.success) {
  background: rgba(16, 185, 129, 0.1);
  padding: 12px 16px;
  border-radius: 6px;
  margin: 12px 0;
  border-left: 3px solid #10b981;
}

.report-footer {
  margin-top: 40px;
}

/* 问答区 */
.qa-section {
  display: flex;
  flex-direction: column;
}

.qa-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.qa-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--text-main);
}

.chat-hint {
  font-size: 0.8rem;
  color: var(--accent-orange);
  background: rgba(255, 107, 53, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}

.qa-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
  overflow-y: auto;
  padding-right: 8px;
  max-height: 500px;
}

.qa-item {
  background: rgba(255, 255, 255, 0.02);
  padding: 16px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.qa-item .q {
  display: block;
  font-weight: bold;
  margin-bottom: 8px;
  color: var(--accent-secondary);
}

.qa-item .a {
  display: block;
  font-size: 0.9rem;
  color: var(--text-muted);
  line-height: 1.6;
}

.qa-item .a :deep(strong) {
  color: var(--accent-orange);
}

.qa-item .a :deep(h1),
.qa-item .a :deep(h2),
.qa-item .a :deep(h3) {
  color: var(--accent-secondary);
  font-size: 1rem;
  margin: 8px 0 4px 0;
}

.qa-item .a :deep(ul),
.qa-item .a :deep(ol) {
  padding-left: 16px;
  color: var(--text-muted);
}
</style>