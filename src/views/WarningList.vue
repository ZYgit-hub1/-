<template>
  <div class="warning-list-page h-full flex flex-col bg-gray-50">
    <!-- 顶部导航 -->
    <div class="bg-gradient-to-r from-yellow-500 to-orange-600 text-white p-4 shadow-lg">
      <div class="container mx-auto">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <el-button @click="$router.back()" circle>
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <h1 class="text-xl font-bold">预警列表</h1>
          </div>
          <el-badge :value="activeCount" :hidden="activeCount === 0" type="warning">
            <el-tag type="warning" effect="dark">{{ activeCount }} 条活跃</el-tag>
          </el-badge>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="bg-white border-b p-4 sticky top-0 z-10">
      <div class="container mx-auto">
        <!-- 预警等级筛选 -->
        <div class="mb-4">
          <div class="text-sm text-gray-600 mb-2">预警等级</div>
          <div class="flex flex-wrap gap-2">
            <el-tag
              v-for="level in warningLevels"
              :key="level.value"
              :type="level.type"
              :effect="selectedLevel === level.value ? 'dark' : 'light'"
              class="cursor-pointer hover:opacity-80 transition-opacity"
              @click="selectedLevel = level.value"
            >
              {{ level.label }}
            </el-tag>
          </div>
        </div>

        <!-- 预警类型筛选 -->
        <div class="mb-4">
          <div class="text-sm text-gray-600 mb-2">预警类型</div>
          <el-select v-model="selectedType" placeholder="全部类型" size="default">
            <el-option label="全部类型" value="all" />
            <el-option label="🌧️ 天气预警" value="weather" />
            <el-option label="🌊 洪水预警" value="flood" />
            <el-option label="🔥 火灾预警" value="fire" />
            <el-option label="⚙️ 设备预警" value="equipment" />
            <el-option label="📋 其他预警" value="other" />
          </el-select>
        </div>

        <!-- 状态筛选 -->
        <div class="flex gap-2">
          <el-button 
            :type="selectedStatus === 'all' ? 'primary' : 'default'"
            size="small"
            @click="selectedStatus = 'all'"
          >
            全部
          </el-button>
          <el-button 
            :type="selectedStatus === 'active' ? 'danger' : 'default'"
            size="small"
            @click="selectedStatus = 'active'"
          >
            活跃
          </el-button>
          <el-button 
            :type="selectedStatus === 'expired' ? 'info' : 'default'"
            size="small"
            @click="selectedStatus = 'expired'"
          >
            已过期
          </el-button>
        </div>
      </div>
    </div>

    <!-- 预警列表 -->
    <div class="flex-1 overflow-auto p-4">
      <div class="container mx-auto">
        <transition-group name="list" tag="div" class="space-y-4">
          <el-card 
            v-for="warning in filteredWarnings" 
            :key="warning.id"
            class="warning-card cursor-pointer hover:shadow-lg transition-all"
            :class="{
              'border-l-4 border-l-red-500': warning.level === 'red',
              'border-l-4 border-l-orange-500': warning.level === 'orange',
              'border-l-4 border-l-yellow-500': warning.level === 'yellow',
              'border-l-4 border-l-blue-500': warning.level === 'blue',
              'border-l-4 border-l-green-500': warning.level === 'green',
              'opacity-60': warning.status === 'expired'
            }"
            @click="handleWarningClick(warning)"
          >
            <div class="flex items-start gap-4">
              <!-- 预警图标 -->
              <div 
                class="flex-shrink-0 w-14 h-14 rounded-lg flex items-center justify-center text-3xl"
                :class="getLevelBgClass(warning.level)"
              >
                {{ getTypeEmoji(warning.type) }}
              </div>
              
              <!-- 预警内容 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between mb-2">
                  <div class="flex items-center gap-2">
                    <el-tag :type="getLevelTagType(warning.level)" size="small" effect="dark">
                      {{ getLevelText(warning.level) }}
                    </el-tag>
                    <el-tag size="small">{{ getTypeText(warning.type) }}</el-tag>
                    <el-tag 
                      :type="warning.status === 'active' ? 'danger' : 'info'" 
                      size="small"
                    >
                      {{ warning.status === 'active' ? '活跃' : '已过期' }}
                    </el-tag>
                  </div>
                </div>
                
                <h3 class="font-bold text-gray-800 mb-1">{{ warning.content }}</h3>
                <div class="text-sm text-gray-600 mb-2">{{ warning.plantName }}</div>
                
                <div class="flex items-center gap-4 text-xs text-gray-500">
                  <span>开始: {{ formatTime(warning.startTime) }}</span>
                  <span v-if="warning.endTime">结束: {{ formatTime(warning.endTime) }}</span>
                  <span v-if="warning.status === 'active'" class="text-red-500 font-semibold">
                    持续 {{ getDuration(warning.startTime) }}
                  </span>
                </div>
              </div>
              
              <!-- 操作按钮 -->
              <div class="flex-shrink-0">
                <el-button 
                  size="small"
                  @click.stop="$router.push(`/plant/${warning.plantId}`)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
          </el-card>
        </transition-group>

        <!-- 空状态 -->
        <el-empty 
          v-if="filteredWarnings.length === 0" 
          description="暂无预警信息"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { mockDataService } from '@/mock/data'
import type { Warning, WarningLevel, WarningType } from '@/types'

const router = useRouter()

const warnings = ref<Warning[]>([])
const selectedLevel = ref<WarningLevel | 'all'>('all')
const selectedType = ref<string>('all')
const selectedStatus = ref<string>('all')

// 预警等级选项
const warningLevels = [
  { label: '全部', value: 'all' as const, type: 'info' as const },
  { label: '🟢 正常', value: 'green' as WarningLevel, type: 'success' as const },
  { label: '🔵 关注', value: 'blue' as WarningLevel, type: 'primary' as const },
  { label: '🟡 警告', value: 'yellow' as WarningLevel, type: 'warning' as const },
  { label: '🟠 严重', value: 'orange' as WarningLevel, type: 'warning' as const },
  { label: '🔴 紧急', value: 'red' as WarningLevel, type: 'danger' as const }
]

// 计算属性
const filteredWarnings = computed(() => {
  let result = warnings.value

  if (selectedLevel.value !== 'all') {
    result = result.filter(w => w.level === selectedLevel.value)
  }

  if (selectedType.value !== 'all') {
    result = result.filter(w => w.type === selectedType.value)
  }

  if (selectedStatus.value !== 'all') {
    result = result.filter(w => w.status === selectedStatus.value)
  }

  return result
})

const activeCount = computed(() => {
  return warnings.value.filter(w => w.status === 'active').length
})

// 预警等级
const getLevelBgClass = (level: WarningLevel) => {
  const classes: Record<WarningLevel, string> = {
    green: 'bg-green-100',
    blue: 'bg-blue-100',
    yellow: 'bg-yellow-100',
    orange: 'bg-orange-100',
    red: 'bg-red-100'
  }
  return classes[level]
}

const getLevelTagType = (level: WarningLevel) => {
  const types: Record<WarningLevel, string> = {
    green: 'success',
    blue: 'primary',
    yellow: 'warning',
    orange: 'warning',
    red: 'danger'
  }
  return types[level]
}

const getLevelText = (level: WarningLevel) => {
  const texts: Record<WarningLevel, string> = {
    green: '正常',
    blue: '关注',
    yellow: '警告',
    orange: '严重',
    red: '紧急'
  }
  return texts[level]
}

// 预警类型
const getTypeEmoji = (type: WarningType) => {
  const emojis: Record<WarningType, string> = {
    weather: '🌧️',
    flood: '🌊',
    fire: '🔥',
    equipment: '⚙️',
    other: '📋'
  }
  return emojis[type]
}

const getTypeText = (type: WarningType) => {
  const texts: Record<WarningType, string> = {
    weather: '天气',
    flood: '洪水',
    fire: '火灾',
    equipment: '设备',
    other: '其他'
  }
  return texts[type]
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取持续时间
const getDuration = (startTime: string) => {
  const start = new Date(startTime)
  const now = new Date()
  const diff = now.getTime() - start.getTime()
  
  const hours = Math.floor(diff / (60 * 60 * 1000))
  if (hours < 24) {
    return `${hours}小时`
  }
  
  const days = Math.floor(hours / 24)
  return `${days}天${hours % 24}小时`
}

// 点击预警
const handleWarningClick = (warning: Warning) => {
  router.push(`/plant/${warning.plantId}`)
}

// 加载数据
onMounted(() => {
  warnings.value = mockDataService.getWarnings()
})
</script>

<style scoped>
.warning-list-page {
  height: 100vh;
}

.container {
  max-width: 1200px;
}

.space-y-4 > * + * {
  margin-top: 1rem;
}

/* 列表动画 */
.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.list-move {
  transition: transform 0.5s ease;
}
</style>
