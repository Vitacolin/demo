<template>
  <n-config-provider :theme="darkTheme" :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <!-- 将原有内容替换为 router-view，由 AppLayout 接管布局 -->
        <router-view />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { 
  darkTheme, 
  NConfigProvider, 
  NMessageProvider, 
  NDialogProvider 
} from 'naive-ui'

// --- 核心：Naive UI 深度主题覆盖 ---
const themeOverrides = {
  common: {
    primaryColor: '#ff6b35',
    primaryColorHover: '#e85d04',
    primaryColorPressed: '#e85d04',
    primaryColorSuppl: 'rgba(255, 107, 53, 0.2)',
    infoColor: '#f5f5dc',
    successColor: '#4ade80',
    warningColor: '#fbbf24',
    errorColor: '#f87171',
    textColorBase: '#e0e0e0',
    textColor1: '#e0e0e0',
    textColor2: '#888888',
    borderRadius: '12px',
    cubicBezierEaseInOut: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
  },
  Input: {
    color: 'rgba(255, 255, 255, 0.05)',
    colorFocus: 'rgba(255, 255, 255, 0.08)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    borderHover: '1px solid #ff6b35',
    borderFocus: '1px solid #ff6b35',
    boxShadowFocus: '0 0 0 2px rgba(255, 107, 53, 0.2)',
    borderRadius: '12px',
  },
  Button: {
    borderRadius: '12px',
    fontWeight: 'bold',
  },
  Card: {
    color: 'rgba(255, 255, 255, 0.03)',
    borderColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: '16px',
  },
  Select: {
    peers: {
      InternalSelection: {
        color: 'rgba(255, 255, 255, 0.05)',
        textColor: '#e0e0e0',
      }
    },
    menuBoxShadow: '0 12px 32px 4px rgba(0, 0, 0, 0.4), 0 8px 20px rgba(0, 0, 0, 0.8)',
    peers_Popover: {
      color: '#1a1a1a',
      textColor: '#e0e0e0'
    }
  },
  Popover: {
    color: '#1a1a1a',
    textColor: '#e0e0e0',
  }
}
</script>

<style>
/* 样式保留在 style.css 中，App.vue 不再需要额外写全局样式 */
</style>
