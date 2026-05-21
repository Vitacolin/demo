<template>
  <div class="page-container">
    <header class="page-header">
      <h2 class="page-title">资产与负债 <span class="highlight">/ Assets</span></h2>
    </header>

    <div class="net-worth-hero aesthetic-card">
      <div class="hero-left">
        <span class="label">当前净资产 (Net Worth)</span>
        <span class="value">¥ {{ assetsData.net_worth.toFixed(2) }}</span>
        <div class="equation">此数据由流水动态计算得出</div>
      </div>
    </div>

    <div class="accounts-grid">
      <!-- 资产区 -->
      <div class="account-section">
        <h3 class="section-title">我的资产</h3>
        <div v-for="(item, idx) in assetsData.assets" :key="idx" class="account-card aesthetic-card">
          <div class="acc-info">
            <span class="acc-icon">{{ item.icon }}</span>
            <span class="acc-name">{{ item.name }}</span>
          </div>
          <span class="acc-bal">¥ {{ item.amount.toFixed(2) }}</span>
        </div>
      </div>

      <!-- 负债区 -->
      <div class="account-section">
        <h3 class="section-title">我的负债</h3>
        <div v-for="(item, idx) in assetsData.debts" :key="idx" class="account-card aesthetic-card debt-card">
          <div class="acc-info">
            <span class="acc-icon">{{ item.icon }}</span>
            <span class="acc-name">{{ item.name }}</span>
          </div>
          <span class="acc-bal debt">- ¥ {{ item.amount.toFixed(2) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

const assetsData = ref({
  net_worth: 0,
  assets: [] as any[],
  debts: [] as any[]
})

const fetchAssetsData = async () => {
  try {
    const res = await axios.get('http://localhost:8000/api/assets')
    assetsData.value = res.data
  } catch (error) {
    console.error("Failed to fetch assets data", error)
  }
}

onMounted(() => {
  fetchAssetsData()
})
</script>

<style scoped>
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

.net-worth-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40px;
  background: linear-gradient(135deg, rgba(255,107,53,0.15) 0%, rgba(18,18,18,0) 100%);
  border-left: 4px solid var(--accent-orange);
}

.hero-left {
  display: flex;
  flex-direction: column;
}

.hero-left .label {
  color: var(--text-muted);
  font-size: 1.1rem;
}

.hero-left .value {
  font-size: 3.5rem;
  font-weight: 900;
  font-family: 'Fira Code', monospace;
  margin: 10px 0;
  text-shadow: 0 4px 12px rgba(255,107,53,0.2);
}

.equation {
  color: var(--text-muted);
  font-size: 0.9rem;
}

.hero-right {
  width: 40%;
}

.mini-chart-placeholder {
  height: 100px;
  border: 1px dashed rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted);
  border-radius: 8px;
}

.accounts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
}

.section-title {
  margin-top: 0;
  margin-bottom: 20px;
  color: var(--text-main);
  font-size: 1.2rem;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  padding-bottom: 10px;
}

.account-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.acc-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.acc-icon {
  font-size: 1.5rem;
}

.acc-name {
  font-size: 1.1rem;
  font-weight: bold;
}

.acc-bal {
  font-size: 1.2rem;
  font-family: 'Fira Code', monospace;
  font-weight: bold;
}

.acc-bal.debt { color: var(--error-color); }
.acc-bal.receive { color: var(--success-color); }

.due {
  font-size: 0.75rem;
  background: rgba(255,255,255,0.1);
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 8px;
  vertical-align: middle;
}
</style>
