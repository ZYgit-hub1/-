<template>
  <div class="alarm-center-page">
    <!-- 顶部头部 -->
    <div class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
        </button>
        <div class="header-title">
          <h1>报警中心</h1>
          <span class="header-subtitle">实时监控告警信息</span>
        </div>
      </div>
      
      <div class="header-right">
        <div class="unread-badge" v-if="unreadCount > 0">
          <span class="badge-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </span>
          <span class="badge-count">{{ unreadCount }}</span>
          <span class="badge-text">条未读</span>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-bar">
      <div class="stats-container">
        <div class="stat-card" @click="activeTab = 'all'">
          <div class="stat-icon all">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
              <line x1="3" y1="9" x2="21" y2="9"/>
              <line x1="9" y1="21" x2="9" y2="9"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ getAlarmCount('all') }}</span>
            <span class="stat-label">全部报警</span>
          </div>
        </div>
        
        <div class="stat-card" :class="{ active: activeTab === 'unconfirmed' }" @click="activeTab = 'unconfirmed'">
          <div class="stat-icon unconfirmed">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value danger">{{ getAlarmCount('unconfirmed') }}</span>
            <span class="stat-label">未确认</span>
          </div>
        </div>
        
        <div class="stat-card" :class="{ active: activeTab === 'confirmed' }" @click="activeTab = 'confirmed'">
          <div class="stat-icon confirmed">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value warning">{{ getAlarmCount('confirmed') }}</span>
            <span class="stat-label">已确认</span>
          </div>
        </div>
        
        <div class="stat-card" :class="{ active: activeTab === 'resolved' }" @click="activeTab = 'resolved'">
          <div class="stat-icon resolved">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <div class="stat-content">
            <span class="stat-value success">{{ getAlarmCount('resolved') }}</span>
            <span class="stat-label">已处置</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="tab-bar">
      <button 
        v-for="tab in tabs" 
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <span class="tab-dot" :class="tab.dotClass"></span>
        <span class="tab-label">{{ tab.label }}</span>
        <span class="tab-count">{{ getAlarmCount(tab.key) }}</span>
      </button>
    </div>

    <!-- 报警列表 -->
    <div class="alarm-list">
      <TransitionGroup name="list">
        <div 
          v-for="alarm in filteredAlarms" 
          :key="alarm.id"
          class="alarm-card"
          :class="[`level-${alarm.level}`, { 'is-unread': alarm.status === 'unconfirmed' }]"
          @click="handleAlarmClick(alarm)"
        >
          <!-- 左侧级别指示 -->
          <div class="alarm-level-indicator">
            <div class="level-icon" :class="`icon-${alarm.level}`">
              <svg v-if="alarm.level === 'emergency'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              <svg v-else-if="alarm.level === 'high'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
              </svg>
              <svg v-else-if="alarm.level === 'medium'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="16" x2="12" y2="12"/>
                <line x1="12" y1="8" x2="12.01" y2="8"/>
              </svg>
            </div>
          </div>

          <!-- 内容区 -->
          <div class="alarm-content">
            <div class="alarm-header">
              <div class="alarm-tags">
                <span class="level-tag" :class="`tag-${alarm.level}`">
                  {{ getLevelText(alarm.level) }}
                </span>
                <span class="status-tag" :class="`status-${alarm.status}`">
                  {{ getStatusText(alarm.status) }}
                </span>
              </div>
              <span class="alarm-time">{{ formatTime(alarm.triggerTime) }}</span>
            </div>
            
            <h3 class="alarm-title">{{ alarm.content }}</h3>
            
            <div class="alarm-meta">
              <span class="meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                {{ alarm.plantName }}
              </span>
              <span v-if="alarm.handler" class="meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
                {{ alarm.handler }}
              </span>
            </div>

            <!-- 时间线 -->
            <div v-if="alarm.confirmTime || alarm.resolveTime" class="alarm-timeline">
              <div v-if="alarm.confirmTime" class="timeline-item">
                <span class="timeline-dot confirmed"></span>
                <span>确认于 {{ formatTime(alarm.confirmTime) }}</span>
              </div>
              <div v-if="alarm.resolveTime" class="timeline-item">
                <span class="timeline-dot resolved"></span>
                <span>处置于 {{ formatTime(alarm.resolveTime) }}</span>
              </div>
            </div>
          </div>

          <!-- 操作区 -->
          <div class="alarm-actions">
            <button 
              v-if="alarm.status === 'unconfirmed'"
              class="action-btn primary"
              @click.stop="handleConfirm(alarm)"
            >
              确认
            </button>
            <button 
              v-if="alarm.status === 'confirmed'"
              class="action-btn success"
              @click.stop="handleResolve(alarm)"
            >
              处置
            </button>
            <button 
              class="action-btn"
              @click.stop="handleAlarmClick(alarm)"
            >
              详情
            </button>
          </div>
        </div>
      </TransitionGroup>

      <!-- 空状态 -->
      <div v-if="filteredAlarms.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <h3>暂无{{ activeTab === 'all' ? '' : getStatusText(activeTab) }}报警</h3>
        <p>所有报警已处理完毕</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mockDataService } from '@/mock/data'
import type { Alarm, AlarmStatus, AlarmLevel } from '@/types'

const router = useRouter()

const alarms = ref<Alarm[]>([])
const activeTab = ref<string>('unconfirmed')
const loading = ref(false)

// Tab 配置
const tabs = [
  { key: 'all', label: '全部', dotClass: 'dot-all' },
  { key: 'unconfirmed', label: '未确认', dotClass: 'dot-danger' },
  { key: 'confirmed', label: '已确认', dotClass: 'dot-warning' },
  { key: 'resolved', label: '已处置', dotClass: 'dot-success' }
]

// 计算属性
const filteredAlarms = computed(() => {
  if (activeTab.value === 'all') {
    return alarms.value
  }
  return alarms.value.filter(a => a.status === activeTab.value)
})

const unreadCount = computed(() => {
  return alarms.value.filter(a => a.status === 'unconfirmed').length
})

// 获取各状态数量
const getAlarmCount = (status: string) => {
  if (status === 'all') {
    return alarms.value.length
  }
  return alarms.value.filter(a => a.status === status).length
}

// 报警级别
const getLevelText = (level: AlarmLevel): string => {
  const texts: Record<AlarmLevel, string> = {
    emergency: '紧急',
    high: '高危',
    medium: '中危',
    low: '低危'
  }
  return texts[level]
}

// 状态
const getStatusText = (status: string): string => {
  const texts: Record<string, string> = {
    unconfirmed: '未确认',
    confirmed: '已确认',
    resolved: '已处置'
  }
  return texts[status] || status
}

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`
  
  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 报警点击
const handleAlarmClick = (alarm: Alarm) => {
  router.push(`/alarm/${alarm.id}`)
}

// 确认报警
const handleConfirm = async (alarm: Alarm) => {
  try {
    await ElMessageBox.confirm('确认此报警信息?', '确认报警', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    alarm.status = 'confirmed'
    alarm.confirmTime = new Date().toISOString()
    alarm.handler = '当前用户'
    
    ElMessage.success('报警已确认')
  } catch {
    // 用户取消
  }
}

// 处置报警
const handleResolve = async (alarm: Alarm) => {
  try {
    await ElMessageBox.confirm('确认已处置此报警?', '处置报警', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'success'
    })
    
    alarm.status = 'resolved'
    alarm.resolveTime = new Date().toISOString()
    
    ElMessage.success('报警已处置')
  } catch {
    // 用户取消
  }
}

// 加载数据
const loadAlarms = async () => {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 300))
    alarms.value = mockDataService.getAlarms()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAlarms()
})
</script>

<style scoped>
.alarm-center-page {
  min-height: 100vh;
  background: #f5f7fa;
}

/* 顶部头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  border-radius: 12px;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateX(-2px);
}

.header-title h1 {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}

.header-subtitle {
  font-size: 14px;
  opacity: 0.85;
}

.header-right {
  display: flex;
  align-items: center;
}

.unread-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  font-size: 14px;
}

.badge-icon {
  display: flex;
}

.badge-count {
  font-weight: 700;
  font-size: 18px;
}

.badge-text {
  opacity: 0.9;
}

/* 统计卡片 */
.stats-bar {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  padding: 16px 24px;
}

.stats-container {
  display: flex;
  gap: 12px;
  max-width: 900px;
  margin: 0 auto;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.stat-card:hover {
  background: #f3f4f6;
}

.stat-card.active {
  border-color: #0ea5e9;
  background: #f0f9ff;
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.stat-icon.all { background: #e0e7ff; color: #4f46e5; }
.stat-icon.unconfirmed { background: #fef2f2; color: #dc2626; }
.stat-icon.confirmed { background: #fef3c7; color: #d97706; }
.stat-icon.resolved { background: #d1fae5; color: #059669; }

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.2;
}

.stat-value.danger { color: #dc2626; }
.stat-value.warning { color: #d97706; }
.stat-value.success { color: #059669; }

.stat-label {
  font-size: 12px;
  color: #6b7280;
}

/* Tab 切换 */
.tab-bar {
  display: flex;
  background: white;
  padding: 0 24px;
  border-bottom: 1px solid #e5e7eb;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 14px;
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
  color: #dc2626;
  border-bottom-color: #dc2626;
}

.tab-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-all { background: #6b7280; }
.dot-danger { background: #dc2626; }
.dot-warning { background: #f59e0b; }
.dot-success { background: #10b981; }

.tab-count {
  padding: 2px 8px;
  background: #f3f4f6;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.tab-item.active .tab-count {
  background: #fef2f2;
  color: #dc2626;
}

/* 报警列表 */
.alarm-list {
  padding: 20px 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.alarm-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 16px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e5e7eb;
  border-left: 4px solid #e5e7eb;
}

.alarm-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.alarm-card.is-unread {
  background: #fffbeb;
}

.alarm-card.level-emergency { border-left-color: #dc2626; }
.alarm-card.level-high { border-left-color: #f97316; }
.alarm-card.level-medium { border-left-color: #f59e0b; }
.alarm-card.level-low { border-left-color: #3b82f6; }

/* 级别指示器 */
.alarm-level-indicator {
  flex-shrink: 0;
}

.level-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.level-icon.icon-emergency { background: #fef2f2; color: #dc2626; }
.level-icon.icon-high { background: #fff7ed; color: #f97316; }
.level-icon.icon-medium { background: #fef3c7; color: #d97706; }
.level-icon.icon-low { background: #eff6ff; color: #3b82f6; }

/* 内容区 */
.alarm-content {
  flex: 1;
  min-width: 0;
}

.alarm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.alarm-tags {
  display: flex;
  gap: 8px;
}

.level-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.tag-emergency { background: #fef2f2; color: #dc2626; }
.tag-high { background: #fff7ed; color: #f97316; }
.tag-medium { background: #fef3c7; color: #d97706; }
.tag-low { background: #eff6ff; color: #3b82f6; }

.status-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.status-unconfirmed { background: #fef2f2; color: #dc2626; }
.status-confirmed { background: #fef3c7; color: #d97706; }
.status-resolved { background: #d1fae5; color: #059669; }

.alarm-time {
  font-size: 12px;
  color: #9ca3af;
}

.alarm-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.alarm-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6b7280;
}

/* 时间线 */
.alarm-timeline {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.timeline-dot.confirmed { background: #f59e0b; }
.timeline-dot.resolved { background: #10b981; }

/* 操作区 */
.alarm-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e5e7eb;
  background: white;
  color: #4b5563;
}

.action-btn:hover {
  background: #f9fafb;
}

.action-btn.primary {
  background: #0ea5e9;
  border-color: #0ea5e9;
  color: white;
}

.action-btn.primary:hover {
  background: #0284c7;
}

.action-btn.success {
  background: #10b981;
  border-color: #10b981;
  color: white;
}

.action-btn.success:hover {
  background: #059669;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #d1fae5;
  border-radius: 50%;
  color: #10b981;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* 加载状态 */
.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  z-index: 100;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e7eb;
  border-top-color: #dc2626;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-overlay span {
  font-size: 14px;
  color: #6b7280;
}

/* 列表动画 */
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-container {
    flex-wrap: wrap;
  }
  
  .stat-card {
    flex-basis: calc(50% - 6px);
  }
  
  .alarm-card {
    flex-direction: column;
  }
  
  .alarm-actions {
    flex-direction: row;
    width: 100%;
  }
  
  .action-btn {
    flex: 1;
  }
}
</style>
