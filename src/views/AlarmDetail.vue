<template>
  <div class="alarm-detail-page">
    <!-- 顶部头部 -->
    <div class="page-header" :class="`header-${alarm?.level || 'low'}`">
      <div class="header-content">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
        </button>
        
        <div class="header-info">
          <h1>报警详情</h1>
          <div class="header-tags">
            <span class="level-tag" :class="`tag-${alarm?.level || 'low'}`">
              {{ getLevelText(alarm?.level) }}
            </span>
            <span class="status-tag" :class="`status-${alarm?.status || 'unconfirmed'}`">
              {{ getStatusText(alarm?.status) }}
            </span>
          </div>
        </div>

        <div class="header-badge" v-if="alarm">
          <div class="badge-icon">
            <svg v-if="alarm.level === 'emergency'" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            <svg v-else-if="alarm.level === 'high'" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
            </svg>
            <svg v-else width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
          </div>
          <span class="badge-text">{{ getLevelText(alarm.level) }}</span>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="page-content" v-if="alarm">
      <!-- 基本信息卡片 -->
      <div class="content-card">
        <div class="card-header">
          <div class="card-title">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="16" x2="12" y2="12"/>
              <line x1="12" y1="8" x2="12.01" y2="8"/>
            </svg>
            <span>报警信息</span>
          </div>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item full-width">
              <label>报警内容</label>
              <div class="info-value highlight">{{ alarm.content }}</div>
            </div>
            <div class="info-item">
              <label>所属电厂</label>
              <div class="info-value link" @click="$router.push(`/plant/${alarm.plantId}`)">
                {{ alarm.plantName }}
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
                  <polyline points="15 3 21 3 21 9"/>
                  <line x1="10" y1="14" x2="21" y2="3"/>
                </svg>
              </div>
            </div>
            <div class="info-item">
              <label>报警编号</label>
              <div class="info-value">{{ alarm.id }}</div>
            </div>
            <div class="info-item">
              <label>触发时间</label>
              <div class="info-value">{{ formatDateTime(alarm.triggerTime) }}</div>
            </div>
            <div class="info-item" v-if="alarm.handler">
              <label>处理人</label>
              <div class="info-value">{{ alarm.handler }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 处置记录卡片 -->
      <div class="content-card">
        <div class="card-header">
          <div class="card-title">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
            </svg>
            <span>处置记录</span>
          </div>
        </div>
        <div class="card-body">
          <div class="timeline">
            <!-- 报警触发 -->
            <div class="timeline-item">
              <div class="timeline-marker danger">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="8" x2="12" y2="12"/>
                  <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
              </div>
              <div class="timeline-content">
                <div class="timeline-title">报警触发</div>
                <div class="timeline-time">{{ formatDateTime(alarm.triggerTime) }}</div>
              </div>
            </div>

            <!-- 报警确认 -->
            <div v-if="alarm.confirmTime" class="timeline-item">
              <div class="timeline-marker warning">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </div>
              <div class="timeline-content">
                <div class="timeline-title">报警确认</div>
                <div class="timeline-time">{{ formatDateTime(alarm.confirmTime) }}</div>
                <div class="timeline-meta" v-if="alarm.handler">处理人：{{ alarm.handler }}</div>
              </div>
            </div>

            <!-- 报警处置 -->
            <div v-if="alarm.resolveTime" class="timeline-item">
              <div class="timeline-marker success">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </div>
              <div class="timeline-content">
                <div class="timeline-title">报警处置</div>
                <div class="timeline-time">{{ formatDateTime(alarm.resolveTime) }}</div>
                <div class="timeline-remark" v-if="alarm.remark">{{ alarm.remark }}</div>
              </div>
            </div>

            <!-- 待处理状态 -->
            <div v-if="alarm.status === 'unconfirmed'" class="timeline-item pending">
              <div class="timeline-marker pending">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
              </div>
              <div class="timeline-content">
                <div class="timeline-title">待处理</div>
                <div class="timeline-hint">等待工作人员确认处理</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 报警规则卡片 -->
      <div class="content-card">
        <div class="card-header">
          <div class="card-title">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="12 2 2 7 12 12 22 7 12 2"/>
              <polyline points="2 17 12 22 22 17"/>
              <polyline points="2 12 12 17 22 12"/>
            </svg>
            <span>报警规则</span>
          </div>
        </div>
        <div class="card-body">
          <div class="rule-list">
            <div class="rule-item">
              <div class="rule-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
              </div>
              <div class="rule-content">
                <div class="rule-label">规则名称</div>
                <div class="rule-value">{{ getRuleName(alarm.level) }}</div>
              </div>
            </div>

            <div class="rule-item">
              <div class="rule-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                </svg>
              </div>
              <div class="rule-content">
                <div class="rule-label">触发条件</div>
                <div class="rule-value">{{ getTriggerCondition(alarm.level) }}</div>
              </div>
            </div>

            <div class="rule-item">
              <div class="rule-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                  <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                </svg>
              </div>
              <div class="rule-content">
                <div class="rule-label">通知方式</div>
                <div class="rule-value">
                  <span class="notify-tag">短信通知</span>
                  <span class="notify-tag">电话通知</span>
                  <span class="notify-tag">APP推送</span>
                </div>
              </div>
            </div>

            <div class="rule-item">
              <div class="rule-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
              </div>
              <div class="rule-content">
                <div class="rule-label">响应时限</div>
                <div class="rule-value">
                  <span :class="alarm.level === 'emergency' ? 'urgent' : ''">
                    {{ getResponseTime(alarm.level) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-bar">
        <button 
          v-if="alarm.status === 'unconfirmed'"
          class="action-btn primary"
          @click="handleConfirm"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          确认报警
        </button>
        
        <button 
          v-if="alarm.status === 'confirmed'"
          class="action-btn success"
          @click="handleResolve"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          标记已处置
        </button>
        
        <button 
          class="action-btn"
          @click="$router.push(`/plant/${alarm.plantId}`)"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          查看电厂详情
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="!alarm" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- 错误状态 -->
    <div v-if="notFound" class="error-state">
      <div class="error-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
      </div>
      <h3>未找到该报警</h3>
      <p>该报警可能已被删除或不存在</p>
      <button @click="$router.back()">返回上一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { mockDataService } from '@/mock/data'
import type { Alarm, AlarmLevel, AlarmStatus } from '@/types'

const route = useRoute()
const router = useRouter()

const alarmId = computed(() => route.params.id as string)
const alarm = ref<Alarm | null>(null)
const notFound = ref(false)

// 报警级别
const getLevelText = (level?: AlarmLevel): string => {
  const texts: Record<AlarmLevel, string> = {
    emergency: '紧急',
    high: '高危',
    medium: '中危',
    low: '低危'
  }
  return texts[level || 'low']
}

// 状态
const getStatusText = (status?: AlarmStatus): string => {
  const texts: Record<AlarmStatus, string> = {
    unconfirmed: '未确认',
    confirmed: '已确认',
    resolved: '已处置'
  }
  return texts[status || 'unconfirmed']
}

// 格式化时间
const formatDateTime = (time: string): string => {
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 获取规则名称
const getRuleName = (level?: AlarmLevel): string => {
  const rules: Record<AlarmLevel, string> = {
    emergency: '紧急报警规则',
    high: '高危报警规则',
    medium: '中危报警规则',
    low: '低危报警规则'
  }
  return rules[level || 'low']
}

// 获取触发条件
const getTriggerCondition = (level?: AlarmLevel): string => {
  const conditions: Record<AlarmLevel, string> = {
    emergency: '水位超过 22m 或温度超过 95°C',
    high: '水位超过 18m 或温度超过 85°C',
    medium: '水位超过 15m 或温度超过 75°C',
    low: '任何监测数据异常'
  }
  return conditions[level || 'low']
}

// 获取响应时限
const getResponseTime = (level?: AlarmLevel): string => {
  const times: Record<AlarmLevel, string> = {
    emergency: '5 分钟内',
    high: '15 分钟内',
    medium: '30 分钟内',
    low: '1 小时内'
  }
  return times[level || 'low']
}

// 确认报警
const handleConfirm = () => {
  if (!alarm.value) return
  
  alarm.value.status = 'confirmed'
  alarm.value.confirmTime = new Date().toISOString()
  alarm.value.handler = '当前用户'
  
  ElMessage.success('报警已确认')
}

// 处置报警
const handleResolve = () => {
  if (!alarm.value) return
  
  alarm.value.status = 'resolved'
  alarm.value.resolveTime = new Date().toISOString()
  alarm.value.remark = '已处理完成'
  
  ElMessage.success('报警已处置')
}

// 加载数据
onMounted(() => {
  const alarms = mockDataService.getAlarms()
  alarm.value = alarms.find(a => a.id === alarmId.value) || alarms[0] || null
  
  if (!alarm.value) {
    notFound.value = true
  }
})
</script>

<style scoped>
.alarm-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
}

/* 顶部头部 */
.page-header {
  padding: 20px 24px;
  color: white;
}

.header-default,
.header-low {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.header-medium {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.header-high {
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
}

.header-emergency {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 800px;
  margin: 0 auto;
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

.header-info {
  flex: 1;
}

.header-info h1 {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.header-tags {
  display: flex;
  gap: 8px;
}

.level-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
}

.tag-emergency { background: rgba(255,255,255,0.25); }
.tag-high { background: rgba(255,255,255,0.2); }
.tag-medium { background: rgba(255,255,255,0.2); }
.tag-low { background: rgba(255,255,255,0.2); }

.status-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  background: rgba(255,255,255,0.25);
}

.header-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  color: #1f2937;
}

.badge-icon {
  margin-bottom: 4px;
}

.header-emergency .badge-icon { color: #dc2626; }
.header-high .badge-icon { color: #f97316; }
.header-medium .badge-icon { color: #f59e0b; }
.header-low .badge-icon { color: #3b82f6; }

.badge-text {
  font-size: 14px;
  font-weight: 700;
}

/* 内容区 */
.page-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.content-card {
  background: white;
  border-radius: 16px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
  background: #f9fafb;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

.card-body {
  padding: 20px;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-item.full-width {
  grid-column: span 2;
}

.info-item label {
  font-size: 13px;
  color: #6b7280;
}

.info-value {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
}

.info-value.highlight {
  font-size: 17px;
  font-weight: 600;
}

.info-value.link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #0ea5e9;
  cursor: pointer;
}

.info-value.link:hover {
  text-decoration: underline;
}

/* 时间线 */
.timeline {
  position: relative;
  padding-left: 30px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 11px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #e5e7eb;
}

.timeline-item {
  position: relative;
  padding-bottom: 24px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-marker {
  position: absolute;
  left: -30px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: white;
}

.timeline-marker.danger { color: #dc2626; }
.timeline-marker.warning { color: #f59e0b; }
.timeline-marker.success { color: #10b981; }
.timeline-marker.pending { color: #6b7280; }

.timeline-content {
  padding-left: 10px;
}

.timeline-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.timeline-time {
  font-size: 13px;
  color: #6b7280;
}

.timeline-meta {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

.timeline-remark {
  font-size: 13px;
  color: #059669;
  margin-top: 4px;
  padding: 6px 10px;
  background: #d1fae5;
  border-radius: 6px;
  display: inline-block;
}

.timeline-hint {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

/* 规则列表 */
.rule-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rule-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
}

.rule-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 10px;
  color: #0ea5e9;
  flex-shrink: 0;
}

.rule-content {
  flex: 1;
}

.rule-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.rule-value {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.rule-value .urgent {
  color: #dc2626;
  font-weight: 700;
}

.notify-tag {
  padding: 4px 10px;
  background: #e0f2fe;
  color: #0284c7;
  border-radius: 6px;
  font-size: 13px;
}

/* 操作按钮 */
.action-bar {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 20px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
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

/* 加载状态 */
.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 16px;
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

.loading-state span {
  font-size: 14px;
  color: #6b7280;
}

.error-icon {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fef2f2;
  border-radius: 50%;
  color: #dc2626;
}

.error-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: #374151;
  margin: 0;
}

.error-state p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.error-state button {
  margin-top: 8px;
  padding: 12px 24px;
  background: #dc2626;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

/* 响应式 */
@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .info-item.full-width {
    grid-column: span 1;
  }
  
  .action-bar {
    flex-direction: column;
  }
}
</style>
