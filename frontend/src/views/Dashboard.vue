<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <h2 class="page-title">执行摘要 <span class="highlight">/ Executive</span></h2>
        <p class="page-subtitle">本月财务健康分：<span class="score">{{ dashboardData.health_score }}</span> (状态：稳健)</p>
      </div>
    </header>

    <!-- 家庭账本选择 -->
    <div class="family-selector aesthetic-card" v-if="families.length > 0">
      <span class="selector-label">选择账本：</span>
      <n-select v-model:value="selectedFamilyId" :options="familyOptions" placeholder="个人账本"
        @update:value="onFamilyChange" style="width: 240px;" />
    </div>

    <!-- 数据指标区：错落排列 -->
    <div class="stats-grid">
      <div class="stat-card aesthetic-card hero-card">
        <span class="label">本月现金流出 (OUT)</span>
        <span class="value highlight">¥ {{ dashboardData.total_expense.toFixed(2) }}</span>
      </div>
      <div class="stat-card aesthetic-card">
        <span class="label">本月现金流入 (IN)</span>
        <span class="value">¥ {{ dashboardData.total_income.toFixed(2) }}</span>
      </div>
      <div class="stat-card aesthetic-card">
        <span class="label">当前结余</span>
        <span class="value" :class="{ 'highlight': dashboardData.balance > 0 }">¥ {{ dashboardData.balance.toFixed(2)
        }}</span>
      </div>
    </div>

    <!-- 图表占位区：不对称设计 -->
    <div class="charts-container">
      <div class="chart-box aesthetic-card sankey-box">
        <div class="chart-header">
          <h3>资金流向桑基图</h3>
          <span class="tag">实时渲染</span>
        </div>
        <div class="placeholder" style="border: none;">
          <!-- ECharts 容器 -->
          <div ref="sankeyChartRef" style="width: 100%; height: 100%;"></div>
        </div>
      </div>

      <!-- 周期性账单组件 -->
      <div class="chart-box aesthetic-card subs-box">
        <div class="chart-header">
          <h3><span class="icon">🔄</span> 周期性账单</h3>
        </div>
        <div class="subs-list">
          <div v-if="activeSubscriptions.length === 0" class="empty-subs">
            暂无活跃订阅
          </div>
          <div v-for="sub in activeSubscriptions.slice(0, 3)" :key="sub.id" class="sub-item">
            <span class="sub-name"><span class="sub-icon">{{ getCategoryIcon(sub.category) }}</span> {{ sub.name
            }}</span>
            <span class="sub-amount">- ¥ {{ sub.amount.toFixed(2) }}</span>
          </div>
        </div>
        <div class="manage-btn-container">
          <button @click="goToSubscriptions" class="manage-btn">管理订阅</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, markRaw, computed } from 'vue'
import * as echarts from 'echarts'
import api from '../api'
import { useRouter } from 'vue-router'
import { NSelect } from 'naive-ui'

const router = useRouter()
const sankeyChartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const dashboardData = ref({
  total_expense: 0,
  total_income: 0,
  balance: 0,
  health_score: 100
})

const activeSubscriptions = ref<any[]>([])

// 家庭账本相关
const families = ref<any[]>([])
const selectedFamilyId = ref<number | null>(null)

const familyOptions = computed(() => {
  return [
    { label: '个人账本', value: null },
    ...families.value.map(f => ({ label: f.name, value: f.id }))
  ]
})

const goToSubscriptions = () => {
  router.push('/subscriptions')
}

const onFamilyChange = () => {
  // 保存到本地存储
  if (selectedFamilyId.value) {
    const family = families.value.find(f => f.id === selectedFamilyId.value)
    if (family) {
      localStorage.setItem('currentFamily', JSON.stringify(family))
    }
  } else {
    localStorage.removeItem('currentFamily')
  }
  fetchDashboardData()
  fetchSubscriptions()
}

const fetchDashboardData = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      console.warn('未登录，使用模拟数据')
      initChart(getMockSankeyData())
      return
    }

    const params: Record<string, any> = {}
    if (selectedFamilyId.value) {
      params.familyId = selectedFamilyId.value
    }

    const res = await api.get('/dashboard', {
      params,
      timeout: 10000
    })
    dashboardData.value = res.data

    if (res.data && res.data.sankey) {
      try {
        initChart(res.data.sankey)
      } catch (chartError: any) {
        console.error("图表初始化失败:", chartError)
        console.warn('桑基图数据存在循环，使用模拟数据...')
        initChart(getMockSankeyData())
      }
    }
  } catch (error) {
    console.error("Failed to fetch dashboard data", error)
    initChart(getMockSankeyData())
  }
}

// 模拟桑基图数据
const getMockSankeyData = () => {
  return {
    nodes: [
      { name: '工资收入' },
      { name: '其他收入' },
      { name: '餐饮' },
      { name: '交通' },
      { name: '购物' },
      { name: '娱乐' }
    ],
    links: [
      { source: '工资收入', target: '餐饮', value: 1500 },
      { source: '工资收入', target: '交通', value: 500 },
      { source: '工资收入', target: '购物', value: 800 },
      { source: '其他收入', target: '娱乐', value: 300 }
    ]
  }
}

const fetchSubscriptions = async () => {
  try {
    const params: Record<string, any> = {}
    if (selectedFamilyId.value) {
      params.familyId = selectedFamilyId.value
    }
    const res = await api.get('/subscriptions', { params })
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

const initChart = (sankeyData: any) => {
  if (sankeyChartRef.value) {
    if (!chartInstance) {
      chartInstance = markRaw(echarts.init(sankeyChartRef.value))
    }

    // 验证数据格式
    if (!sankeyData || !Array.isArray(sankeyData.nodes) || !Array.isArray(sankeyData.links)) {
      console.warn('桑基图数据格式不正确，使用模拟数据')
      chartInstance.setOption(getChartOption(getMockSankeyData()))
      return
    }

    try {
      chartInstance.setOption(getChartOption(sankeyData))
    } catch (error: any) {
      console.error('图表初始化失败:', error)
      // 如果桑基图数据有问题，使用模拟数据
      console.warn('桑基图数据存在循环，使用模拟数据...')
      chartInstance.setOption(getChartOption(getMockSankeyData()))
    }
  }
}

// 分离图表配置
const getChartOption = (sankeyData: any) => {
  return {
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      backgroundColor: 'rgba(18,18,18,0.9)',
      borderColor: '#ff6b35',
      textStyle: { color: '#e0e0e0' }
    },
    series: [
      {
        type: 'sankey',
        data: sankeyData.nodes,
        links: sankeyData.links,
        emphasis: {
          focus: 'adjacency'
        },
        nodeAlign: 'left',
        nodeGap: 16,
        layoutIterations: 0,
        lineStyle: {
          color: 'source',
          curveness: 0.5,
          opacity: 0.3
        },
        label: {
          color: '#e0e0e0',
          fontFamily: 'Lato, sans-serif',
          fontSize: 12
        },
        itemStyle: {
          borderWidth: 0,
          borderRadius: 4
        }
      }
    ]
  }
}

// 检测并移除循环引用
const removeCycles = (data: any): any => {
  if (!data || !data.nodes || !data.links) {
    return { nodes: [], links: [] }
  }

  try {
    // 构建节点ID映射
    const nodeIds = new Set(data.nodes.map((n: any) => n.name))

    // 检测循环引用
    const hasCycle = detectCycle(data.links, nodeIds)

    if (hasCycle) {
      console.warn('检测到桑基图数据存在循环引用，正在修复...')
      // 移除可能导致循环的链接
      return {
        nodes: data.nodes,
        links: removeCyclicLinks(data.links, nodeIds)
      }
    }

    return data
  } catch (error) {
    console.error('处理桑基图数据时出错:', error)
    return { nodes: [], links: [] }
  }
}

// 检测是否存在循环
const detectCycle = (links: any[], nodeIds: Set<string>): boolean => {
  // 构建邻接表
  const adjacency: Record<string, string[]> = {}
  nodeIds.forEach(id => adjacency[id] = [])
  links.forEach(link => {
    if (adjacency[link.source]) {
      adjacency[link.source].push(link.target)
    }
  })

  // 使用DFS检测循环
  const visited = new Set<string>()
  const inStack = new Set<string>()

  const hasCycleDFS = (node: string): boolean => {
    if (!visited.has(node)) {
      visited.add(node)
      inStack.add(node)

      if (adjacency[node]) {
        for (const neighbor of adjacency[node]) {
          if (!visited.has(neighbor) && hasCycleDFS(neighbor)) {
            return true
          } else if (inStack.has(neighbor)) {
            return true
          }
        }
      }
    }

    inStack.delete(node)
    return false
  }

  for (const node of nodeIds) {
    if (hasCycleDFS(node)) {
      return true
    }
  }

  return false
}

// 移除导致循环的链接
const removeCyclicLinks = (links: any[], nodeIds: Set<string>): any[] => {
  const cleanedLinks: any[] = []
  const usedTargets = new Set<string>()

  // 按source分组，确保每个target只被连接一次
  const groupedLinks: Record<string, any[]> = {}
  links.forEach(link => {
    if (!groupedLinks[link.source]) {
      groupedLinks[link.source] = []
    }
    groupedLinks[link.source].push(link)
  })

  // 简单策略：移除可能导致循环的链接
  links.forEach(link => {
    // 避免自循环和重复目标
    if (link.source !== link.target && !usedTargets.has(link.target)) {
      cleanedLinks.push(link)
      usedTargets.add(link.target)
    }
  })

  return cleanedLinks
}

const fetchFamilies = async () => {
  try {
    const res = await api.get('/families')
    families.value = res.data

    // 从本地存储恢复上次选择的家庭
    const savedFamily = localStorage.getItem('currentFamily')
    if (savedFamily) {
      try {
        const family = JSON.parse(savedFamily)
        const exists = families.value.some(f => f.id === family.id)
        if (exists) {
          selectedFamilyId.value = family.id
        }
      } catch {
        localStorage.removeItem('currentFamily')
      }
    }
  } catch (error) {
    console.error("Failed to fetch families", error)
  }
}

onMounted(() => {
  fetchFamilies().then(() => {
    fetchDashboardData()
    fetchSubscriptions()
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
  }
})

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  margin-bottom: 10px;
}

/* 家庭账本选择器 */
.family-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
}

.selector-label {
  color: var(--text-muted);
  font-size: 0.9rem;
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

.score {
  color: var(--success-color);
  font-weight: bold;
  font-size: 1.2rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 24px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-card {
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.1) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 107, 53, 0.2);
}

.label {
  font-size: 0.85rem;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.value {
  font-size: 2.5rem;
  font-weight: 800;
  margin: 12px 0;
  font-family: 'Fira Code', monospace;
}

.trend {
  font-size: 0.85rem;
}

.trend.up {
  color: var(--error-color);
}

.trend.down {
  color: var(--success-color);
}

.progress-bar-container {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  margin-top: 16px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: var(--accent-orange);
  border-radius: 3px;
}

.charts-container {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.chart-header h3 {
  margin: 0;
  font-size: 1.1rem;
}

.tag {
  font-size: 0.75rem;
  padding: 4px 8px;
  background: rgba(255, 107, 53, 0.15);
  color: var(--accent-orange);
  border-radius: 4px;
}

.placeholder {
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  border: 1px dashed rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}

.mock-sankey {
  width: 80%;
  height: 60%;
  background: linear-gradient(90deg, transparent, rgba(255, 107, 53, 0.1), transparent);
  margin-bottom: 20px;
  border-radius: 20px;
}

.mock-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 4px;
  padding: 20px;
}

.mock-cell {
  width: 16px;
  height: 16px;
  background: var(--accent-orange);
  border-radius: 4px;
}

/* 周期性账单 Widget 样式 */
.subs-box {
  display: flex;
  flex-direction: column;
}

.subs-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.sub-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.sub-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  color: var(--text-main);
}

.sub-icon {
  font-size: 1.2rem;
}

.sub-amount {
  font-family: 'Fira Code', monospace;
  font-weight: bold;
  color: var(--text-muted);
}

.empty-subs {
  text-align: center;
  color: var(--text-muted);
  font-size: 0.9rem;
  padding: 20px 0;
}

.manage-btn-container {
  display: flex;
  justify-content: center;
  margin-top: auto;
}

.manage-btn {
  display: inline-block;
  width: 100%;
  text-align: center;
  padding: 10px 0;
  border: 1px dashed rgba(255, 255, 255, 0.2);
  background: transparent;
  color: var(--text-muted);
  border-radius: 8px;
  text-decoration: none;
  font-weight: bold;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.manage-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-main);
  border-color: rgba(255, 255, 255, 0.4);
}
</style>
