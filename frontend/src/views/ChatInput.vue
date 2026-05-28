<template>
  <div class="page-container">
    <header class="page-header">
      <h2 class="page-title">多模态智能录入 <span class="highlight">/ Input</span></h2>
      <p class="page-subtitle">支持自然语言、OCR票据、语音识别的超级入口</p>
    </header>

    <!-- 家庭账本选择 -->
    <div class="family-selector aesthetic-card" v-if="families.length > 0">
      <span class="selector-label">选择账本：</span>
      <n-select v-model:value="selectedFamilyId" :options="familyOptions" placeholder="个人账本" style="width: 240px;" />
    </div>

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
                <div v-if="msg.duplicateInfo" class="duplicate-alert">
                  <n-alert type="warning" :title="msg.duplicateInfo.title" closable>
                    <template #default>
                      <div v-html="msg.duplicateInfo.message.replace(/\n/g, '<br/>')"></div>
                      <div
                        v-if="msg.duplicateInfo.potentialDuplicates && msg.duplicateInfo.potentialDuplicates.length > 0"
                        class="duplicate-list">
                        <p style="margin-top: 12px; font-weight: bold;">检测到的重复账单：</p>
                        <ul style="margin: 8px 0 0 20px;">
                          <li v-for="(dup, idx) in msg.duplicateInfo.potentialDuplicates" :key="idx">
                            {{ dup.description }} - ¥{{ dup.amount.toFixed(2) }} ({{ formatDate(dup.date) }})
                          </li>
                        </ul>
                      </div>
                      <div style="margin-top: 12px;">
                        <n-button size="small" type="primary" @click="confirmDuplicateSubmit(index)">
                          确认添加重复账单
                        </n-button>
                      </div>
                    </template>
                  </n-alert>
                </div>
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
          <n-input v-model:value="inputText" type="textarea" placeholder="告诉 AI 你花了什么..."
            :autosize="{ minRows: 3, maxRows: 5 }" @keydown.enter.prevent="sendMessage" :disabled="isLoading" />
          <div class="actions">
            <n-button ghost @click="toggleVoiceInput" :type="isRecording ? 'warning' : 'default'">
              {{ isRecording ? '🔴 录音中...' : '🎙️ 语音输入' }}
            </n-button>
            <n-button type="primary" @click="sendMessage" :loading="isLoading">发送解析</n-button>
          </div>
        </div>
      </div>

      <!-- 右侧：OCR与多模态区 -->
      <div class="side-section">
        <div class="ocr-card aesthetic-card">
          <h3>📸 票据智能识别</h3>
          <p class="desc">上传购物小票或餐饮发票，AI 自动提取明细。</p>

          <!-- 使用 on-change 替代 before-upload，因为 before-upload 时文件可能还未完全加载 -->
          <n-upload directory-dnd accept="image/*" :show-file-list="false" @change="handleOcrChange">
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
import { ref, nextTick, onMounted, computed } from 'vue'
import { NInput, NButton, NUpload, NSelect, useMessage } from 'naive-ui'
import api from '../api'
import { useRouter } from 'vue-router'

// 引入 Tesseract.js 用于 OCR
import Tesseract from 'tesseract.js'

const inputText = ref('')
const isLoading = ref(false)
const isOcrScanning = ref(false)
const messages = ref<{ role: 'user' | 'ai', content: string, duplicateInfo?: { title: string, message: string, potentialDuplicates: any[], pendingTransactions: any[] } }[]>([])
const chatHistoryRef = ref<HTMLElement | null>(null)
const message = useMessage()
const router = useRouter()

// 家庭账本相关
const families = ref<any[]>([])
const selectedFamilyId = ref<number | null>(null)
const familyOptions = computed(() => {
  return [
    { label: '个人账本', value: null },
    ...families.value.map(f => ({ label: f.name, value: f.id }))
  ]
})

const activeSubscriptions = ref<any[]>([])
const isRecording = ref(false)
let recognition: any = null

// 检查浏览器是否支持语音识别
const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition

const fetchSubscriptions = async () => {
  try {
    const res = await api.get('/subscriptions')
    activeSubscriptions.value = res.data.filter((sub: any) => sub.is_active)
  } catch (error) {
    console.error("Failed to fetch subscriptions", error)
  }
}

const fetchFamilies = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return

    const res = await api.get('/families')
    families.value = res.data

    // 从本地存储恢复之前选中的家庭
    const savedFamily = localStorage.getItem('currentFamily')
    if (savedFamily) {
      const family = JSON.parse(savedFamily)
      if (families.value.some(f => f.id === family.id)) {
        selectedFamilyId.value = family.id
      }
    }
  } catch (error) {
    console.error('获取家庭列表失败', error)
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
  fetchFamilies()
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

// 语音输入功能
const toggleVoiceInput = () => {
  if (!SpeechRecognition) {
    message.warning('当前浏览器不支持语音识别功能，请使用 Chrome 或 Edge 浏览器')
    return
  }

  if (isRecording.value) {
    // 停止录音
    stopRecording()
  } else {
    // 开始录音
    startRecording()
  }
}

const startRecording = () => {
  try {
    recognition = new SpeechRecognition()
    recognition.continuous = false // 松手后停止
    recognition.interimResults = true // 显示中间结果
    recognition.lang = 'zh-CN' // 设置为中文

    recognition.onstart = () => {
      isRecording.value = true
      message.success('开始语音识别，请说话...')
    }

    recognition.onresult = (event: any) => {
      const results = event.results
      const transcript = results[results.length - 1][0].transcript

      // 实时更新输入框
      inputText.value = transcript

      // 如果是最终结果
      if (results[results.length - 1].isFinal) {
        message.success('语音识别完成！')
      }
    }

    recognition.onerror = (event: any) => {
      console.error('语音识别错误:', event.error)
      isRecording.value = false

      if (event.error === 'not-allowed') {
        message.error('请允许麦克风权限后重试')
      } else if (event.error === 'no-speech') {
        message.warning('未检测到语音，请重试')
      } else {
        message.error('语音识别出错: ' + event.error)
      }
    }

    recognition.onend = () => {
      isRecording.value = false
    }

    recognition.start()
  } catch (error) {
    console.error('启动语音识别失败:', error)
    message.error('语音识别启动失败，请检查浏览器权限设置')
    isRecording.value = false
  }
}

const stopRecording = () => {
  if (recognition) {
    recognition.stop()
    isRecording.value = false
  }
}

// OCR 处理函数 - 处理 NUpload 的 on-change 事件
const handleOcrChange = (options: { file: any; fileList: any[] }) => {
  console.log('OCR on-change 触发:', options)

  // 等待文件完全加载后再处理
  // NUpload 的 on-change 会被调用多次：1. file 添加时 2. file 上传成功/失败时
  // 我们需要等待 fileList 有内容且 file 有 url 或 status 为 'uploading'
  setTimeout(async () => {
    const { file, fileList } = options

    console.log('处理文件:', file.name, '状态:', file.status, 'fileList长度:', fileList.length)

    // 等待文件完全准备好
    // 当 status 为 'uploading' 或 'done' 时，file 对象应该包含完整的文件信息
    if (file.status === 'uploading' || file.status === 'pending') {
      // 文件正在上传或等待上传，此时可能还没有完整的 File 对象
      // 尝试从 file 对象中获取文件
      let actualFile: File | null = null

      // NUpload 的 file 对象可能包含以下属性之一
      if (file.file instanceof File) {
        actualFile = file.file
      } else if (file.raw instanceof File) {
        actualFile = file.raw
      } else if (file.thumbnailUrl) {
        // 如果有 thumbnailUrl，可以尝试使用它
        console.log('文件有 thumbnailUrl，尝试使用')
      }

      // 如果找到文件，调用 OCR 处理
      if (actualFile) {
        await processFileWithOCR(actualFile)
      } else {
        // 文件还未准备好，等待一下再试
        console.log('文件还未准备好，等待...')
        setTimeout(() => {
          // 再次尝试获取文件
          const fileInList = fileList.find(f => f.id === file.id)
          if (fileInList && (fileInList.file instanceof File || fileInList.raw instanceof File)) {
            const readyFile = (fileInList.file || fileInList.raw) as File
            processFileWithOCR(readyFile)
          }
        }, 100)
      }
    }
  }, 50)
}

// 处理文件进行 OCR 识别
const processFileWithOCR = async (file: File) => {
  isOcrScanning.value = true
  let imageDataUrl: string | null = null

  try {
    console.log('开始OCR处理文件:', file.name, file.type, file.size)

    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      message.error('请上传图片文件')
      isOcrScanning.value = false
      return
    }

    // 验证文件大小（不超过10MB）
    if (file.size > 10 * 1024 * 1024) {
      message.error('图片大小不能超过10MB')
      isOcrScanning.value = false
      return
    }

    // 使用 FileReader.readAsDataURL 方法
    imageDataUrl = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        const result = e.target?.result as string
        if (result) {
          resolve(result)
        } else {
          reject(new Error('Failed to read file'))
        }
      }
      reader.onerror = () => reject(new Error('FileReader error'))
      reader.readAsDataURL(file)
    })

    console.log('图片读取成功，数据URL长度:', imageDataUrl.length)

    // 使用 Tesseract.js 进行图片识别
    // 针对中文表格优化配置
    const result = await Tesseract.recognize(
      imageDataUrl,
      'chi_sim+eng', // 同时识别中文和英文数字
      {
        logger: (info: any) => {
          console.log(`OCR进度: ${info.status} ${info.progress * 100}%`)
        },
        // 优化配置
        tessedit_pageseg_mode: '6', // 假设单一统一文本块
        tessedit_char_blacklist: '',
        preserve_interword_spaces: '1',
      }
    )

    // 提取识别到的文本
    let ocrText = result.data.text.trim()
    console.log('OCR原始识别结果:', ocrText)

    // 如果识别结果为空或太短，尝试使用图像预处理
    if (!ocrText || ocrText.length < 5) {
      console.log('OCR识别结果为空或太短，尝试优化...')

      // 尝试使用不同的配置重新识别
      const result2 = await Tesseract.recognize(
        imageDataUrl,
        'chi_sim',
        {
          logger: (info: any) => console.log(`OCR重试: ${info.progress * 100}%`),
          tessedit_pageseg_mode: '3', // 自动页面分割
        }
      )
      ocrText = result2.data.text.trim()
      console.log('OCR重试识别结果:', ocrText)
    }

    // 如果仍然识别不到，提示用户手动输入
    if (!ocrText || ocrText.length < 5) {
      message.warning('图片文字识别困难，请确保图片清晰。您可以尝试手动输入账单信息。')
      isOcrScanning.value = false
      return
    }

    // 清理识别结果（去除多余空白和特殊字符）
    ocrText = cleanupOcrText(ocrText)
    console.log('清理后的OCR结果:', ocrText)

    // 将识别出的文本发送到聊天框进行 LLM 解析
    inputText.value = ocrText
    message.success("票据信息提取成功，正在交由 AI 解析...")
    await sendMessage()
  } catch (error: any) {
    console.error('OCR识别失败:', error)
    message.error('图片识别失败，请尝试手动输入账单信息')
  } finally {
    imageDataUrl = null
    isOcrScanning.value = false
  }
}

// 清理OCR识别结果
const cleanupOcrText = (text: string): string => {
  // 去除多余的空白字符
  text = text.replace(/\s+/g, ' ')

  // 去除特殊字符
  text = text.replace(/[^\u4e00-\u9fa5a-zA-Z0-9\u0020\u3000，。！？、：；（）]/g, '')

  // 替换全角空格为半角空格
  text = text.replace(/\u3000/g, ' ')

  // 去除首尾空白
  text = text.trim()

  return text
}

// 真实的 OCR 识别流程（保留作为备用，但主要使用 on-change）
const handleOcrUpload = async (data: any) => {
  isOcrScanning.value = true
  let imageDataUrl: string | null = null

  try {
    // NUpload 的 before-upload 回调参数可能是不同的格式
    let file: File | null = null

    // 尝试多种可能的文件获取方式
    if (data && data.file) {
      file = data.file
    } else if (data instanceof File) {
      file = data
    } else if (data && data.fileList && data.fileList.length > 0) {
      // 处理 NUpload 的 fileList 格式
      const fileInfo = data.fileList[0]
      if (fileInfo) {
        // 尝试从 fileInfo 中获取文件
        file = fileInfo.file || fileInfo.raw || fileInfo
        // 如果是响应式对象，需要访问 .value
        if (file && typeof file === 'object' && 'value' in file) {
          file = (file as any).value
        }
      }
    } else if (data && typeof data === 'object') {
      // 尝试从data对象中找到file属性
      for (const key in data) {
        const value = data[key]
        if (value instanceof File) {
          file = value
          break
        }
        // 尝试访问响应式对象的 .value
        if (value && typeof value === 'object' && 'value' in value) {
          const innerFile = value.value
          if (innerFile instanceof File) {
            file = innerFile
            break
          }
        }
      }
    }

    // 验证文件
    if (!file || !(file instanceof File)) {
      console.error('无法获取文件，数据内容:', data, '提取的文件:', file)
      message.error('无法获取文件，请重新选择')
      isOcrScanning.value = false
      return false
    }

    console.log('获取到文件:', file.name, file.type || 'unknown', file.size)

    // 验证文件类型
    const fileType = file.type || (file.name ? file.name.split('.').pop()?.toLowerCase() : '')
    if (fileType && !fileType.startsWith('image/') && !['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(fileType)) {
      message.error('请上传图片文件')
      isOcrScanning.value = false
      return false
    }

    // 验证文件大小（不超过10MB）
    if (file.size > 10 * 1024 * 1024) {
      message.error('图片大小不能超过10MB')
      isOcrScanning.value = false
      return false
    }

    // 使用 FileReader.readAsDataURL 方法（更可靠）
    imageDataUrl = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        const result = e.target?.result as string
        if (result) {
          resolve(result)
        } else {
          reject(new Error('Failed to read file'))
        }
      }
      reader.onerror = (e) => {
        reject(new Error('FileReader error: ' + e.type))
      }
      // 如果 file 不是真正的 File 对象，尝试转换
      try {
        reader.readAsDataURL(file)
      } catch (err) {
        reject(err)
      }
    })

    console.log('图片读取成功，数据URL长度:', imageDataUrl.length)

    // 使用 Tesseract.js 进行图片识别
    const result = await Tesseract.recognize(
      imageDataUrl,
      'chi_sim',
      {
        logger: (info: any) => {
          console.log(`OCR进度: ${info.status} ${info.progress * 100}%`)
        }
      }
    )

    // 提取识别到的文本
    const ocrText = result.data.text.trim()
    console.log('OCR识别结果:', ocrText)

    // 如果识别结果为空，提示用户
    if (!ocrText) {
      message.warning('未能识别到文字，请确保图片清晰且包含文字')
      isOcrScanning.value = false
      return false
    }

    // 将识别出的文本发送到聊天框进行 LLM 解析
    inputText.value = ocrText
    message.success("票据信息提取成功，正在交由 AI 解析...")
    await sendMessage()
  } catch (error: any) {
    console.error('OCR识别失败:', error)
    // 提供更友好的错误提示
    if (error.message && error.message.includes('read image')) {
      message.error('图片读取失败，请尝试使用其他图片或重新上传')
    } else if (error.message && error.message.includes('readAsDataURL')) {
      message.error('文件读取失败，请检查文件格式')
    } else if (error.message && error.message.includes('FileReader')) {
      message.error('文件处理失败，请尝试其他图片')
    } else {
      message.error('图片识别失败，请重试')
    }
  } finally {
    // 清理
    imageDataUrl = null
    isOcrScanning.value = false
  }

  return false
}

// 将文件转换为 base64
const fileToBase64 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const sendMessage = async () => {
  if (!inputText.value.trim()) return

  const userText = inputText.value
  messages.value.push({ role: 'user', content: userText })
  inputText.value = ''
  isLoading.value = true
  await scrollToBottom()

  try {
    const token = localStorage.getItem('token')
    if (!token) {
      messages.value.push({
        role: 'ai',
        content: "请先登录后再使用智能记账功能"
      })
      isLoading.value = false
      await scrollToBottom()
      return
    }

    const response = await api.post('/chat', {
      text: userText,
      model: 'qwen3:8b',
      familyId: selectedFamilyId.value
    }, {
      timeout: 30000
    })

    const aiMessage: { role: 'user' | 'ai', content: string, duplicateInfo?: any } = {
      role: 'ai',
      content: response.data.message
    }

    if (response.data.hasDuplicates && response.data.duplicateMessage) {
      aiMessage.duplicateInfo = {
        title: '⚠️ 检测到重复账单',
        message: response.data.duplicateMessage,
        potentialDuplicates: response.data.potentialDuplicates || [],
        pendingTransactions: response.data.parsed_transactions || []
      }
      message.warning('检测到重复账单，已跳过相关记录')
    } else if (response.data.parsed_transactions && response.data.parsed_transactions.length > 0) {
      message.success(`成功记录 ${response.data.parsed_transactions.length} 笔账单！`)
    }

    messages.value.push(aiMessage)
  } catch (error: any) {
    console.error('发送消息失败:', error)
    if (error.response && error.response.status === 401) {
      messages.value.push({
        role: 'ai',
        content: "登录已过期，请重新登录"
      })
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else {
      messages.value.push({
        role: 'ai',
        content: "抱歉，服务器请求失败，请检查后端和 Ollama 是否正常运行。"
      })
      message.error('网络请求失败')
    }
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

const confirmDuplicateSubmit = async (messageIndex: number) => {
  const msg = messages.value[messageIndex]
  if (!msg.duplicateInfo || !msg.duplicateInfo.pendingTransactions.length) {
    return
  }

  message.info('正在确认添加重复账单...')

  try {
    const token = localStorage.getItem('token')
    if (!token) {
      message.error('请先登录')
      return
    }

    for (const tx of msg.duplicateInfo.pendingTransactions) {
      const txWithFamily = {
        ...tx,
        familyId: selectedFamilyId.value
      }
      await api.post('/transactions', txWithFamily)
    }

    msg.duplicateInfo = undefined
    msg.content += '\n\n✅ 已确认添加所有重复账单'
    message.success(`成功添加 ${msg.duplicateInfo?.pendingTransactions.length || 0} 笔重复账单`)
  } catch (error: any) {
    console.error('确认重复账单失败:', error)
    message.error('添加失败，请重试')
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}
</script>

<style scoped>
/* 原有样式保留 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.page-header {
  margin-bottom: 10px;
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

.family-selector {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
}

.selector-label {
  color: var(--text-muted);
}

.input-grid {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 30px;
  height: calc(100vh - 280px);
}

.chat-section {
  display: flex;
  flex-direction: column;
  padding: 24px;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 24px;
  padding-right: 12px;
  scroll-behavior: smooth;
}

.msg {
  display: flex;
  gap: 16px;
  max-width: 85%;
}

.msg.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.avatar {
  font-size: 1.5rem;
  background: rgba(255, 255, 255, 0.05);
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  flex-shrink: 0;
}

.bubble {
  background: rgba(255, 255, 255, 0.05);
  padding: 16px 20px;
  border-radius: 16px;
  line-height: 1.6;
  position: relative;
  word-break: break-all;
}

.msg.ai .bubble {
  border-top-left-radius: 4px;
  border-left: 2px solid var(--accent-orange);
}

.msg.user .bubble {
  background: rgba(255, 107, 53, 0.1);
  border-top-right-radius: 4px;
}

.input-box {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.actions {
  display: flex;
  justify-content: space-between;
}

.side-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.ocr-card h3,
.recurring-card h3 {
  margin-top: 0;
  font-size: 1.1rem;
}

.desc {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-bottom: 20px;
}

.upload-area {
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
  overflow: hidden;
}

.upload-area:hover {
  border-color: var(--accent-orange);
  background: rgba(255, 107, 53, 0.05);
  color: var(--text-main);
}

.upload-area.is-scanning {
  border-color: var(--accent-orange);
  background: rgba(255, 107, 53, 0.1);
}

.scanning-animation {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--accent-orange);
  font-weight: bold;
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--accent-orange);
  box-shadow: 0 0 10px var(--accent-orange);
  animation: scan 1.5s infinite linear;
}

@keyframes scan {
  0% {
    top: 0;
  }

  50% {
    top: 100%;
  }

  100% {
    top: 0;
  }
}

.sub-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sub-list li {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 0.95rem;
}

.sub-list li:last-child {
  border-bottom: none;
}

.sub-list .amt {
  font-family: 'Fira Code', monospace;
  font-weight: bold;
}

/* Loading 动画 */
.loading-bubble {
  display: flex;
  gap: 4px;
  align-items: center;
  height: 24px;
}

.dot {
  width: 6px;
  height: 6px;
  background: var(--accent-orange);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {

  0%,
  80%,
  100% {
    transform: scale(0);
  }

  40% {
    transform: scale(1);
  }
}
</style>
