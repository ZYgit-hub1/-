<template>
  <div class="statistics-page h-full overflow-auto bg-gray-50">
    <!-- 顶部导航 -->
    <div class="bg-gradient-to-r from-blue-600 to-blue-800 text-white p-4 shadow-lg">
      <div class="container mx-auto">
        <div class="flex items-center gap-4">
          <el-button @click="$router.back()" circle>
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <h1 class="text-xl font-bold">数据统计分析</h1>
        </div>
      </div>
    </div>

    <!-- 统计内容 -->
    <div class="container mx-auto p-4">
      <!-- 概览卡片 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="bg-white rounded-lg p-6 shadow">
          <div class="text-sm text-gray-600 mb-2">电厂总数</div>
          <div class="text-3xl font-bold text-blue-600">{{ stats.totalPlants }}</div>
        </div>
        <div class="bg-white rounded-lg p-6 shadow">
          <div class="text-sm text-gray-600 mb-2">活跃报警</div>
          <div class="text-3xl font-bold text-red-600">{{ stats.totalAlarms }}</div>
        </div>
        <div class="bg-white rounded-lg p-6 shadow">
          <div class="text-sm text-gray-600 mb-2">未读报警</div>
          <div class="text-3xl font-bold text-orange-600">{{ stats.unreadAlarms }}</div>
        </div>
        <div class="bg-white rounded-lg p-6 shadow">
          <div class="text-sm text-gray-600 mb-2">预警数量</div>
          <div class="text-3xl font-bold text-yellow-600">{{ stats.activeWarnings }}</div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <!-- 电厂类型分布 -->
        <el-card>
          <template #header>
            <span class="font-bold">电厂类型分布</span>
          </template>
          <div ref="typeChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>

        <!-- 报警级别分布 -->
        <el-card>
          <template #header>
            <span class="font-bold">报警级别分布</span>
          </template>
          <div ref="alarmLevelChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>
      </div>

      <!-- 趋势图 -->
      <el-card class="mb-6">
        <template #header>
          <span class="font-bold">报警趋势 (近7天)</span>
        </template>
        <div ref="trendChartRef" style="width: 100%; height: 300px;"></div>
      </el-card>

      <!-- 电厂状态列表 -->
      <el-card>
        <template #header>
          <span class="font-bold">电厂运行状态</span>
        </template>
        <el-table :data="plantStatusList" stripe style="width: 100%">
          <el-table-column prop="name" label="电厂名称" width="200" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <span>{{ getPlantTypeText(row.type) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="capacity" label="容量 (MW)" width="120" />
          <el-table-column prop="alarms" label="报警数" width="100" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" @click="$router.push(`/plant/${row.id}`)">
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { mockDataService } from '@/mock/data'
import type { DashboardStats, PowerPlant } from '@/types'

const router = useRouter()

const stats = ref<DashboardStats>({
  totalPlants: 0,
  normalPlants: 0,
  warningPlants: 0,
  dangerPlants: 0,
  totalAlarms: 0,
  unreadAlarms: 0,
  activeWarnings: 0
})

const plants = ref<PowerPlant[]>([])
const plantStatusList = ref<any[]>([])

const typeChartRef = ref<HTMLElement | null>(null)
const alarmLevelChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)

let typeChart: echarts.ECharts | null = null
let alarmLevelChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

// 电厂类型文本
const getPlantTypeText = (type: string) => {
  const texts: Record<string, string> = {
    coal: '火电',
    gas: '燃气',
    solar: '光伏',
    wind: '风电',
    storage: '储能'
  }
  return texts[type] || type
}

// 状态类型
const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    normal: 'success',
    warning: 'warning',
    danger: 'danger',
    offline: 'info'
  }
  return types[status] || 'info'
}

// 状态文本
const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    normal: '正常',
    warning: '预警',
    danger: '危险',
    offline: '离线'
  }
  return texts[status] || status
}

// 初始化类型分布图
const initTypeChart = () => {
  if (!typeChartRef.value) return

  typeChart = echarts.init(typeChartRef.value)

  const typeCount = {
    coal: 0,
    gas: 0,
    solar: 0,
    wind: 0,
    storage: 0
  }

  plants.value.forEach(p => {
    typeCount[p.type]++
  })

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{c}个'
        },
        data: [
          { value: typeCount.coal, name: '火电', itemStyle: { color: '#ef4444' } },
          { value: typeCount.gas, name: '燃气', itemStyle: { color: '#f97316' } },
          { value: typeCount.solar, name: '光伏', itemStyle: { color: '#eab308' } },
          { value: typeCount.wind, name: '风电', itemStyle: { color: '#22c55e' } },
          { value: typeCount.storage, name: '储能', itemStyle: { color: '#3b82f6' } }
        ]
      }
    ]
  }

  typeChart.setOption(option)
}

// 初始化报警级别分布图
const initAlarmLevelChart = () => {
  if (!alarmLevelChartRef.value) return

  alarmLevelChart = echarts.init(alarmLevelChartRef.value)

  const levelCount = {
    emergency: 0,
    high: 0,
    medium: 0,
    low: 0
  }

  mockDataService.getAlarms().forEach(a => {
    levelCount[a.level]++
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: ['紧急', '高', '中', '低']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        type: 'bar',
        data: [
          {
            value: levelCount.emergency,
            itemStyle: { color: '#ef4444' }
          },
          {
            value: levelCount.high,
            itemStyle: { color: '#f97316' }
          },
          {
            value: levelCount.medium,
            itemStyle: { color: '#eab308' }
          },
          {
            value: levelCount.low,
            itemStyle: { color: '#3b82f6' }
          }
        ],
        barWidth: '50%',
        itemStyle: {
          borderRadius: [8, 8, 0, 0]
        }
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    }
  }

  alarmLevelChart.setOption(option)
}

// 初始化趋势图
const initTrendChart = () => {
  if (!trendChartRef.value) return

  trendChart = echarts.init(trendChartRef.value)

  // 生成近7天数据
  const days = []
  const alarmCounts = []
  const now = new Date()

  for (let i = 6; i >= 0; i--) {
    const date = new Date(now.getTime() - i * 24 * 60 * 60 * 1000)
    days.push(`${date.getMonth() + 1}/${date.getDate()}`)
    alarmCounts.push(Math.floor(Math.random() * 20) + 5)
  }

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['报警数量']
    },
    xAxis: {
      type: 'category',
      data: days,
      boundaryGap: false
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '报警数量',
        type: 'line',
        smooth: true,
        data: alarmCounts,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.5)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.1)' }
          ])
        },
        lineStyle: {
          color: '#3b82f6',
          width: 3
        },
        itemStyle: {
          color: '#3b82f6'
        }
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    }
  }

  trendChart.setOption(option)
}

// 加载数据
const loadData = () => {
  stats.value = mockDataService.getStats()
  plants.value = mockDataService.getPlants()

  plantStatusList.value = plants.value.slice(0, 10).map(p => ({
    ...p,
    alarms: Math.floor(Math.random() * 5)
  }))

  // 初始化图表
  initTypeChart()
  initAlarmLevelChart()
  initTrendChart()
}

// 窗口大小变化时调整图表
const handleResize = () => {
  typeChart?.resize()
  alarmLevelChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  typeChart?.dispose()
  alarmLevelChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.statistics-page {
  min-height: 100vh;
}

.container {
  max-width: 1400px;
}

.grid {
  display: grid;
}
</style>
