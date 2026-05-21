<template>
  <div class="page-container">
    <header class="page-header">
      <h2 class="page-title">多模态智能录入 <span class="highlight">/ Input</span></h2>
      <p class="page-subtitle">支持自然语言、OCR票据、语音识别的超级入口</p>
    </header>

    <div class="input-grid">
      <!-- 左侧：AI 对话核心区 -->
      <div class="chat-section aesthetic-card">
        <div class="chat-history" ref="chatHistoryRef">
          <div class="msg ai">
            <span class="avatar">🤖</span>
            <div class="bubble">欢迎回来。今天有什么需要记录的账单吗？您可以直接说“昨天打车花了35块”。</div>
          </div>
          
          <template v-for="(msg, index) in messages" :key="index">
            <div :class="['msg', msg.role]">
              <span class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</span>
              <div class="bubble">
                <div v-html="msg.content.replace(/\n/g, '<br/>')"></div>
              </div>
            </div>
          </template>
          
          <!-- Loading 状态 -->
          <div v-if="isLoading" class="msg ai">
            <span class="avatar">🤖</span>
            <div class="bubble loading-bubble">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>

        <div class="input-box">
          <n-input 
            v-model:value="inputText"
            type="textarea" 
            placeholder="告诉 AI 你花了什么..."
            :autosize="{ minRows: 3, maxRows: 5 }"
            @keydown.enter.prevent="sendMessage"
            :disabled="isLoading"
          />
          <div class="actions">
            <n-button ghost>🎙️ 语音输入</n-button>
            <n-button type="primary" @click="sendMessage" :loading="isLoading">发送解析</n-button>
          </div>
        </div>
      </div>

      <!-- 右侧：OCR与多模态区 -->
      <div class="side-section">
        <div class="ocr-card aesthetic-card">
          <h3>📸 票据智能识别</h3>
          <p class="desc">上传购物小票或餐饮发票，AI 自动提取明细。</p>
          
          <!-- 真实的上传组件替代原来的纯占位符 -->
          <n-upload
            directory-dnd
            accept="image/*"
            :show-file-list="false"
            @before-upload="handleOcrUpload"
          >
            <div class="upload-area" :class="{ 'is-scanning': isOcrScanning }">
              <span class="icon" v-if="!isOcrScanning">📤</span>
              <span v-if="!isOcrScanning">拖拽图片至此，或点击上传</span>
              
              <div v-if="isOcrScanning" class="scanning-animation">
                <span class="scan-line"></span>
                <span>正在提取票据信息...</span>
              </div>
            </div>
          </n-upload>
        </div>

        <div class="recurring-card aesthetic-card">
          <h3>🔄 周期性账单</h3>
          <ul class="sub-list">
            <li v-if="activeSubscriptions.length === 0" style="justify-content: center; color: var(--text-muted);">
              暂无活跃订阅
            </li>
            <li v-for="sub in activeSubscriptions.slice(0, 3)" :key="sub.id">
              <span>{{ getCategoryIcon(sub.category) }} {{ sub.name }}</span>
              <span class="amt">- ¥ {{ sub.amount.toFixed(2) }}</span>
            </li>
          </ul>
          <n-button size="small" dashed block style="margin-top: 16px;" @click="goToSubscriptions">管理订阅</n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { NInput, NButton, NUpload, useMessage } from 'naive-ui'
import axios from 'axios'
import { useRouter } from 'vue-router'

const inputText = ref('')
const isLoading = ref(false)
const isOcrScanning = ref(false)
const messages = ref<{role: 'user' | 'ai', content: string}[]>([])
const chatHistoryRef = ref<HTMLElement | null>(null)
const message = useMessage()
const router = useRouter()

// 后端 API 地址
const API_BASE_URL = 'http://localhost:8000'

const activeSubscriptions = ref<any[]>([])

const fetchSubscriptions = async () => {
  try {
    const res = await axios.get(`${API_BASE_URL}/api/subscriptions`)
    activeSubscriptions.value = res.data.filter((sub: any) => sub.is_active)
  } catch (error) {
    console.error("Failed to fetch subscriptions", error)
  }
}

const getCategoryIcon = (cat: string) => {
  const icons: Record<string, string> = {
    '娱乐': '🎵', '住房': '🏠', '服务': '🔧', '保险': '🛡️', '其他': '📦'
  }
  return icons[cat] || '🏷️'
}

onMounted(() => {
  fetchSubscriptions()
})

const goToSubscriptions = () => {
  router.push('/subscriptions')
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatHistoryRef.value) {
    chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
  }
}

// 模拟 OCR 识别流程
const handleOcrUpload = async (data: { file: File }) => {
  isOcrScanning.value = true
  
  // 模拟图片识别耗时 2.5 秒
  setTimeout(async () => {
    isOcrScanning.value = false
    
    // 模拟从图片中提取出了一段文本
    const mockOcrText = "帮我记录超市小票：进口零食大礼包 85.50元，海飞丝洗发水500ml 42.00元，环保购物袋 0.50元。应付总金额 128.00元。"
    
    // 将提取出的文本直接发送到聊天框进行 LLM 解析
    inputText.value = mockOcrText
    message.success("票据信息提取成功，已自动填入对话框，正在交由 AI 解析...")
    await sendMessage()
  }, 2500)
  
  return false // 阻止默认上传行为，因为我们没有后端接收图片
}

const sendMessage = async () => {
  if (!inputText.value.trim()) return
  
  const userText = inputText.value
  messages.value.push({ role: 'user', content: userText })
  inputText.value = ''
  isLoading.value = true
  await scrollToBottom()

  try {
    const response = await axios.post(`${API_BASE_URL}/api/chat`, {
      text: userText,
      model: 'qwen2.5:7b'
    })
    
    messages.value.push({ 
      role: 'ai', 
      content: response.data.message 
    })
    
    if (response.data.parsed_transactions.length > 0) {
      message.success(`成功记录 ${response.data.parsed_transactions.length} 笔账单！`)
    }
  } catch (error: any) {
    console.error(error)
    messages.value.push({ 
      role: 'ai', 
      content: "抱歉，服务器请求失败，请检查后端和 Ollama 是否正常运行。" 
    })
    message.error('网络请求失败')
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}
</script>

<style scoped>
/* 原有样式保留 */
.page-container { display: flex; flex-direction: column; gap: 30px; }
.page-header { margin-bottom: 10px; }
.page-title { font-size: 2rem; margin: 0; font-weight: 800; }
.page-subtitle { color: var(--text-muted); margin-top: 8px; }
.input-grid { display: grid; grid-template-columns: 1fr 350px; gap: 30px; height: calc(100vh - 200px); }
.chat-section { display: flex; flex-direction: column; padding: 24px; }
.chat-history { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 24px; margin-bottom: 24px; padding-right: 12px; scroll-behavior: smooth; }
.msg { display: flex; gap: 16px; max-width: 85%; }
.msg.user { align-self: flex-end; flex-direction: row-reverse; }
.avatar { font-size: 1.5rem; background: rgba(255,255,255,0.05); width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; border-radius: 50%; flex-shrink: 0;}
.bubble { background: rgba(255,255,255,0.05); padding: 16px 20px; border-radius: 16px; line-height: 1.6; position: relative; word-break: break-all; }
.msg.ai .bubble { border-top-left-radius: 4px; border-left: 2px solid var(--accent-orange); }
.msg.user .bubble { background: rgba(255,107,53,0.1); border-top-right-radius: 4px; }
.input-box { display: flex; flex-direction: column; gap: 16px; }
.actions { display: flex; justify-content: space-between; }
.side-section { display: flex; flex-direction: column; gap: 24px; }
.ocr-card h3, .recurring-card h3 { margin-top: 0; font-size: 1.1rem; }
.desc { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 20px; }
.upload-area { border: 1px dashed rgba(255,255,255,0.2); border-radius: 12px; height: 120px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: var(--text-muted); cursor: pointer; transition: all var(--transition-fast); position: relative; overflow: hidden; }
.upload-area:hover { border-color: var(--accent-orange); background: rgba(255,107,53,0.05); color: var(--text-main); }
.upload-area.is-scanning { border-color: var(--accent-orange); background: rgba(255,107,53,0.1); }
.scanning-animation { display: flex; flex-direction: column; align-items: center; gap: 12px; color: var(--accent-orange); font-weight: bold; }
.scan-line { position: absolute; top: 0; left: 0; width: 100%; height: 2px; background: var(--accent-orange); box-shadow: 0 0 10px var(--accent-orange); animation: scan 1.5s infinite linear; }
@keyframes scan { 0% { top: 0; } 50% { top: 100%; } 100% { top: 0; } }
.sub-list { list-style: none; padding: 0; margin: 0; }
.sub-list li { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 0.95rem; }
.sub-list li:last-child { border-bottom: none; }
.sub-list .amt { font-family: 'Fira Code', monospace; font-weight: bold; }

/* Loading 动画 */
.loading-bubble { display: flex; gap: 4px; align-items: center; height: 24px; }
.dot { width: 6px; height: 6px; background: var(--accent-orange); border-radius: 50%; animation: bounce 1.4s infinite ease-in-out both; }
.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
</style>
