<template>
  <div class="filter-bar">
    <!-- 筛选面板 -->
    <div class="filter-panel">
      <!-- 预警等级筛选 -->
      <div class="filter-section">
        <div class="filter-label">
          <span>预警等级</span>
          <button 
            v-if="selectedWarningLevel !== 'all'" 
            class="clear-btn"
            @click="clearWarningLevel"
          >
            清除
          </button>
        </div>
        <div class="filter-tags">
          <button
            v-for="level in warningLevelOptions"
            :key="level.value"
            class="filter-tag"
            :class="{ active: selectedWarningLevel === level.value }"
            :style="selectedWarningLevel === level.value ? { backgroundColor: level.color, borderColor: level.color } : {}"
            @click="toggleWarningLevel(level.value)"
          >
            <span class="tag-dot" :style="{ backgroundColor: level.color }" />
            <span>{{ level.label }}</span>
            <span class="tag-count">{{ level.count }}</span>
          </button>
        </div>
      </div>

      <!-- 电厂类型筛选 -->
      <div class="filter-section">
        <div class="filter-label">
          <span>电厂类型</span>
          <button 
            v-if="selectedType !== 'all'" 
            class="clear-btn"
            @click="clearType"
          >
            清除
          </button>
        </div>
        <div class="filter-tags">
          <button
            v-for="type in plantTypeOptions"
            :key="type.value"
            class="filter-tag type-tag"
            :class="{ active: selectedType === type.value }"
            @click="toggleType(type.value)"
          >
            <span class="tag-icon">{{ type.icon }}</span>
            <span>{{ type.label }}</span>
            <span class="tag-count">{{ type.count }}</span>
          </button>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="filter-actions">
        <button class="reset-btn" @click="resetFilters">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
            <path d="M3 3v5h5"/>
          </svg>
          重置
        </button>
        <button 
          class="close-btn"
          @click="emit('close')"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 已选筛选器标签 -->
    <div v-if="hasActiveFilters" class="active-filters">
      <span class="active-filters-label">已选筛选：</span>
      <div class="active-filter-tags">
        <span 
          v-if="selectedWarningLevel !== 'all'" 
          class="active-filter-tag"
          :style="{ backgroundColor: getWarningColor(selectedWarningLevel) + '20', color: getWarningColor(selectedWarningLevel) }"
        >
          {{ getWarningLabel(selectedWarningLevel) }}
          <button @click="clearWarningLevel">×</button>
        </span>
        <span 
          v-if="selectedType !== 'all'" 
          class="active-filter-tag type"
        >
          {{ getTypeLabel(selectedType) }}
          <button @click="clearType">×</button>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { WARNING_COLORS } from '@/composables/useAmap'
import type { PowerPlant, WarningLevel } from '@/types'

const props = defineProps<{
  plants: PowerPlant[]
}>()

const emit = defineEmits<{
  (e: 'filter-change', filters: FilterState): void
  (e: 'close'): void
}>()

export interface FilterState {
  warningLevel: WarningLevel | 'all'
  type: string
}

// 筛选状态
const selectedWarningLevel = ref<WarningLevel | 'all'>('all')
const selectedType = ref<string>('all')

// 预警等级选项
const warningLevelOptions = computed(() => {
  const levels: Array<{ value: WarningLevel | 'all'; label: string; color: string; count: number }> = [
    { value: 'green', label: '正常', color: WARNING_COLORS.green, count: 0 },
    { value: 'blue', label: '关注', color: WARNING_COLORS.blue, count: 0 },
    { value: 'yellow', label: '警告', color: WARNING_COLORS.yellow, count: 0 },
    { value: 'orange', label: '严重', color: WARNING_COLORS.orange, count: 0 },
    { value: 'red', label: '紧急', color: WARNING_COLORS.red, count: 0 }
  ]
  
  levels.forEach(level => {
    level.count = props.plants.filter(p => p.warningLevel === level.value).length
  })
  
  return levels
})

// 电厂类型选项
const plantTypeOptions = computed(() => {
  const types = [
    { value: 'coal', label: '火电', icon: '🏭' },
    { value: 'gas', label: '燃气', icon: '⚡' },
    { value: 'solar', label: '光伏', icon: '☀️' },
    { value: 'wind', label: '风电', icon: '🌬️' },
    { value: 'storage', label: '储能', icon: '🔋' }
  ]
  
  return types.map(type => ({
    ...type,
    count: props.plants.filter(p => p.type === type.value).length
  }))
})

// 是否有激活的筛选
const hasActiveFilters = computed(() => 
  selectedWarningLevel.value !== 'all' || selectedType.value !== 'all'
)

// 切换预警等级
function toggleWarningLevel(level: WarningLevel | 'all') {
  selectedWarningLevel.value = level
  emitFilterChange()
}

// 切换电厂类型
function toggleType(type: string) {
  selectedType.value = type
  emitFilterChange()
}

// 清除预警等级筛选
function clearWarningLevel() {
  selectedWarningLevel.value = 'all'
  emitFilterChange()
}

// 清除类型筛选
function clearType() {
  selectedType.value = 'all'
  emitFilterChange()
}

// 重置所有筛选
function resetFilters() {
  selectedWarningLevel.value = 'all'
  selectedType.value = 'all'
  emitFilterChange()
}

// 发送筛选变更事件
function emitFilterChange() {
  emit('filter-change', {
    warningLevel: selectedWarningLevel.value,
    type: selectedType.value
  })
}

// 获取预警等级颜色
function getWarningColor(level: WarningLevel | 'all'): string {
  if (level === 'all') return '#6b7280'
  return WARNING_COLORS[level]
}

// 获取预警等级标签
function getWarningLabel(level: WarningLevel | 'all'): string {
  if (level === 'all') return '全部'
  const labels: Record<WarningLevel, string> = {
    green: '正常',
    blue: '关注',
    yellow: '警告',
    orange: '严重',
    red: '紧急'
  }
  return labels[level]
}

// 获取类型标签
function getTypeLabel(type: string): string {
  if (type === 'all') return '全部'
  const types = plantTypeOptions.value
  return types.find(t => t.value === type)?.label || type
}

// 监听 plants 变化时重新计算计数
watch(() => props.plants, () => {
  // 触发重新计算 computed 属性
}, { deep: true })

// 暴露方法
defineExpose({
  resetFilters,
  clearWarningLevel,
  clearType,
  getFilters: (): FilterState => ({
    warningLevel: selectedWarningLevel.value,
    type: selectedType.value
  })
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-panel {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

.filter-section {
  margin-bottom: 16px;
}

.filter-section:last-of-type {
  margin-bottom: 12px;
}

.filter-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 10px;
}

.clear-btn {
  font-size: 12px;
  font-weight: 500;
  color: #9ca3af;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.clear-btn:hover {
  color: #ef4444;
  background: #fef2f2;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 12px;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tag:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.filter-tag.active {
  color: white;
  border-color: transparent;
}

.filter-tag.active .tag-count {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.type-tag .tag-icon {
  font-size: 14px;
}

.tag-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 1.5px solid white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.tag-count {
  background: #e5e7eb;
  color: #6b7280;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
}

.filter-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.reset-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.reset-btn:hover {
  background: #e5e7eb;
  color: #4b5563;
}

.close-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fef2f2;
  border: none;
  border-radius: 6px;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #fee2e2;
}

/* 已选筛选器标签 */
.active-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 8px;
  font-size: 12px;
}

.active-filters-label {
  color: #0369a1;
  font-weight: 500;
}

.active-filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.active-filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: white;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.active-filter-tag.type {
  background: #fef3c7;
  color: #92400e;
}

.active-filter-tag button {
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.1);
  border: none;
  border-radius: 50%;
  font-size: 10px;
  cursor: pointer;
  margin-left: 2px;
}

.active-filter-tag button:hover {
  background: rgba(0, 0, 0, 0.2);
}
</style>
