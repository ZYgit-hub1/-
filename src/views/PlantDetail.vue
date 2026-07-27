<template>
  <div class="plant-detail-page">
    <!-- 顶部头部 -->
    <div class="detail-header" :class="getHeaderClass()">
      <div class="header-content">
        <!-- 返回按钮和信息 -->
        <div class="header-top">
          <button class="back-btn" @click="$router.back()">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
          </button>
          
          <div class="header-info">
            <div class="plant-name-row">
              <h1 class="plant-name">{{ plant?.name || '电厂详情' }}</h1>
              <span class="plant-icon">{{ getPlantIcon() }}</span>
            </div>
            <div class="plant-meta">
              <span class="meta-item">
                <span class="meta-icon">🏭</span>
                {{ getPlantTypeName() }}
              </span>
              <span class="meta-divider">|</span>
              <span class="meta-item">
                <span class="meta-icon">⚡</span>
                {{ plant?.capacity || '--' }} MW
              </span>
              <span class="meta-divider">|</span>
              <span class="meta-item">
                <span class="meta-icon">📍</span>
                {{ plant?.address || '暂无地址' }}
              </span>
            </div>
          </div>

          <!-- 预警等级徽章 -->
          <div class="warning-badge" :class="`level-${plant?.warningLevel || 'green'}`">
            <div class="badge-icon">
              <svg v-if="plant?.warningLevel === 'red'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              <svg v-else-if="['orange', 'yellow'].includes(plant?.warningLevel || '')" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>
            <div class="badge-content">
              <span class="badge-label">{{ getWarningLevelText() }}</span>
              <span class="badge-sublabel">{{ getStatusText() }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tabs 导航 -->
    <div class="tabs-wrapper">
      <div class="tabs-container">
        <button 
          v-for="tab in tabs" 
          :key="tab.name"
          class="tab-item"
          :class="{ active: activeTab === tab.name }"
          @click="activeTab = tab.name"
        >
          <span class="tab-icon">{{ tab.icon }}</span>
          <span class="tab-label">{{ tab.label }}</span>
        </button>
      </div>
    </div>

    <!-- Tab 内容区 -->
    <div class="tab-content">
      <!-- 气象信息 -->
      <div v-show="activeTab === 'weather'" class="tab-panel">
        <WeatherOverview 
          :plant-id="plantId" 
          :weather="weatherData" 
        />
      </div>

      <!-- 水文状态 -->
      <div v-show="activeTab === 'hydro'" class="tab-panel">
        <HydroStatus 
          :plant-id="plantId" 
          :hydro-data="hydroData" 
        />
      </div>

      <!-- 应急响应 -->
      <div v-show="activeTab === 'emergency'" class="tab-panel">
        <EmergencyResponse 
          :plant-id="plantId" 
          :emergency="emergencyData" 
        />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner">
        <div class="spinner-ring"></div>
        <span>加载中...</span>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error && !plant" class="error-state">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="12" cy="12" r="10"/>
        <path d="M16 16s-1.5-2-4-2-4 2-4 2"/>
        <line x1="9" y1="9" x2="9.01" y2="9"/>
        <line x1="15" y1="9" x2="15.01" y2="9"/>
      </svg>
      <h3>未找到该电厂信息</h3>
      <p>可能已被删除或链接已失效</p>
      <button @click="$router.back()">返回上一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { mockDataService } from '@/mock/data'
import type { PowerPlant, WeatherData, HydroData, EmergencyResponse as EmergencyResponseData } from '@/types'
import WeatherOverview from '@/components/WeatherOverview.vue'
import HydroStatus from '@/components/HydroStatus.vue'
import EmergencyResponse from '@/components/EmergencyResponse.vue'

const route = useRoute()
const plantId = computed(() => route.params.id as string)

const plant = ref<PowerPlant | null>(null)
const weatherData = ref<WeatherData | null>(null)
const hydroData = ref<HydroData | null>(null)
const emergencyData = ref<EmergencyResponseData | null>(null)
const activeTab = ref('weather')
const loading = ref(false)
const error = ref(false)

// Tab 配置
const tabs = [
  { name: 'weather', label: '气象信息', icon: '🌤️' },
  { name: 'hydro', label: '水文状态', icon: '🌊' },
  { name: 'emergency', label: '应急响应', icon: '🚨' }
]

// 获取电厂类型名称
function getPlantTypeName(): string {
  if (!plant.value) return '--'
  const names: Record<string, string> = {
    coal: '火电厂',
    gas: '燃气电厂',
    solar: '光伏电站',
    wind: '风电场',
    storage: '储能站'
  }
  return names[plant.value.type] || plant.value.type
}

// 获取电厂图标
function getPlantIcon(): string {
  if (!plant.value) return '🏭'
  const icons: Record<string, string> = {
    coal: '🏭',
    gas: '⚡',
    solar: '☀️',
    wind: '🌬️',
    storage: '🔋'
  }
  return icons[plant.value.type] || '🏭'
}

// 获取预警等级文本
function getWarningLevelText(): string {
  if (!plant.value) return '正常'
  const texts: Record<string, string> = {
    green: '正常',
    blue: '关注',
    yellow: '警告',
    orange: '严重',
    red: '紧急'
  }
  return texts[plant.value.warningLevel || 'green']
}

// 获取状态文本
function getStatusText(): string {
  if (!plant.value) return '--'
  const texts: Record<string, string> = {
    normal: '正常运行',
    warning: '预警中',
    danger: '危险',
    offline: '离线'
  }
  return texts[plant.value.status] || plant.value.status
}

// 获取头部样式类
function getHeaderClass(): string {
  if (!plant.value) return 'header-default'
  const level = plant.value.warningLevel || 'green'
  return `header-${level}`
}

// 加载数据
const loadData = async () => {
  loading.value = true
  error.value = false
  
  try {
    await new Promise(resolve => setTimeout(resolve, 300))
    
    plant.value = mockDataService.getPlantById(plantId.value) || null
    
    if (!plant.value) {
      error.value = true
      return
    }
    
    weatherData.value = mockDataService.getWeatherData(plantId.value)
    hydroData.value = mockDataService.getHydroData(plantId.value)
    
    // 根据预警等级生成应急响应数据
    const level = plant.value.warningLevel || 'green'
    const levelMap: Record<string, { plan: string; status: 'standby' | 'activated' | 'ended' }> = {
      green: { plan: '三级响应', status: 'standby' },
      blue: { plan: '三级响应', status: 'standby' },
      yellow: { plan: '三级响应', status: 'activated' },
      orange: { plan: '二级响应', status: 'activated' },
      red: { plan: '一级响应', status: 'activated' }
    }
    
    const config = levelMap[level] || levelMap.green
    
    emergencyData.value = {
      planLevel: config.plan,
      status: config.status,
      commander: level === 'red' ? '李总指挥' : level === 'orange' ? '王指挥官' : '张调度员',
      contactPhone: '400-' + String(Math.floor(Math.random() * 9000 + 1000)) + '-999',
      activationTime: config.status === 'activated' ? new Date().toISOString() : undefined,
      measures: [
        '启动应急预案',
        '加强设备巡检频率',
        '实时监测气象数据变化',
        '准备应急物资和设备',
        '通知相关部门协调',
        '安排值班人员到位'
      ]
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.plant-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
}

/* 顶部头部 */
.detail-header {
  padding: 20px 24px;
  transition: all 0.3s ease;
}

.header-default,
.header-green {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.header-blue {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.header-yellow {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.header-orange {
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
}

.header-red {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  animation: header-pulse 2s ease-in-out infinite;
}

@keyframes header-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.9; }
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
}

.header-top {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

/* 返回按钮 */
.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 12px;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(-2px);
}

/* 头部信息 */
.header-info {
  flex: 1;
  min-width: 0;
}

.plant-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.plant-name {
  font-size: 28px;
  font-weight: 700;
  color: white;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.plant-icon {
  font-size: 28px;
}

.plant-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-icon {
  font-size: 16px;
}

.meta-divider {
  opacity: 0.5;
}

/* 预警等级徽章 */
.warning-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.warning-badge.level-green .badge-icon { color: #10b981; }
.warning-badge.level-blue .badge-icon { color: #3b82f6; }
.warning-badge.level-yellow .badge-icon { color: #f59e0b; }
.warning-badge.level-orange .badge-icon { color: #f97316; }
.warning-badge.level-red .badge-icon { color: #ef4444; }

.badge-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-content {
  display: flex;
  flex-direction: column;
}

.badge-label {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.2;
}

.badge-sublabel {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

/* Tab 导航 */
.tabs-wrapper {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tabs-container {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  gap: 8px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 15px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-item:hover {
  color: #374151;
  background: #f9fafb;
}

.tab-item.active {
  color: #0ea5e9;
  border-bottom-color: #0ea5e9;
}

.tab-icon {
  font-size: 20px;
}

.tab-label {
  font-weight: 600;
}

/* Tab 内容区 */
.tab-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.tab-panel {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 加载状态 */
.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.spinner-ring {
  width: 48px;
  height: 48px;
  border: 4px solid #e5e7eb;
  border-top-color: #0ea5e9;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-spinner span {
  font-size: 14px;
  color: #6b7280;
}

/* 错误状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: #6b7280;
  text-align: center;
  padding: 40px;
}

.error-state svg {
  margin-bottom: 24px;
  opacity: 0.5;
}

.error-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 8px 0;
}

.error-state p {
  font-size: 14px;
  margin: 0 0 24px 0;
}

.error-state button {
  padding: 12px 24px;
  background: #0ea5e9;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.error-state button:hover {
  background: #0284c7;
}

/* 响应式 */
@media (max-width: 768px) {
  .detail-header {
    padding: 16px;
  }
  
  .header-top {
    flex-direction: column;
    gap: 16px;
  }
  
  .plant-name {
    font-size: 22px;
  }
  
  .warning-badge {
    width: 100%;
    justify-content: center;
  }
  
  .tabs-container {
    padding: 0 16px;
    overflow-x: auto;
  }
  
  .tab-item {
    padding: 12px 16px;
    white-space: nowrap;
  }
  
  .tab-content {
    padding: 16px;
  }
}
</style>
