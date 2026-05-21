<template>
  <div class="page-container">
    <header class="page-header">
      <h2 class="page-title">专属财务顾问 <span class="highlight">/ AI Advisor</span></h2>
      <div class="header-actions">
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
            <span class="period-tag" v-if="periodLabel">{{ periodLabel }}</span>
            <span class="date">{{ new Date().toLocaleString() }}</span>
          </div>
        </div>

        <div class="report-body">
          <div v-if="loading" class="loading-text">
            正在分析您的财务数据并生成报告... 请稍候...
          </div>
          <div v-else-if="report" class="greeting" style="white-space: pre-wrap;" v-html="renderMarkdown(report)"></div>
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
        <n-input
          v-model:value="chatInput"
          type="textarea"
          placeholder="对报告有修改意见？或提出理财问题..."
          :autosize="{minRows: 2}"
          @keydown.enter.prevent="sendChat"
          :disabled="chatLoading"
        />
        <n-button
          type="primary"
          style="margin-top: 12px;"
          @click="sendChat"
          :loading="chatLoading"
        >
          发送问题
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { NButton, NInput, useMessage } from 'naive-ui'
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

onMounted(() => {
  setQuickRange('month')
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
    const params: Record<string, string> = { persona: persona.value }
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await api.get('/api/advisor', { params })
    report.value = res.data.report
  } catch (e: any) {
    message.error('生成报告失败，请检查后端和大模型是否正常运行')
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
    const payload = {
      question,
      persona: persona.value,
      currentReport: report.value || ''
    }
    const res = await api.post('/api/advisor/chat', payload)
    const answer = res.data.answer
    const isReportUpdate = res.data.isReportUpdate

    chatHistory.value[chatHistory.value.length - 1].a = answer

    // 如果是报告局部修改，自动更新报告内容
    if (isReportUpdate && report.value) {
      report.value = answer
    }
  } catch (e: any) {
    chatHistory.value[chatHistory.value.length - 1].a = '抱歉，请求失败，请检查后端和大模型是否正常运行。'
    message.error('请求失败')
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
  top: 0; left: 0; right: 0; height: 3px;
  background: linear-gradient(90deg, transparent, var(--accent-orange), transparent);
  animation: scanline 2s linear infinite;
}

@keyframes scanline {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
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

.greeting {
  font-size: 1.05rem;
  color: var(--text-main);
  line-height: 1.8;
}

.greeting :deep(h1),
.greeting :deep(h2),
.greeting :deep(h3) {
  color: var(--accent-secondary);
  margin-top: 16px;
  margin-bottom: 8px;
}

.greeting :deep(ul),
.greeting :deep(ol) {
  padding-left: 20px;
  color: var(--text-main);
}

.greeting :deep(strong) {
  color: var(--accent-orange);
}

.greeting :deep(p) {
  margin-bottom: 12px;
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