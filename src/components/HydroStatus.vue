<template>
  <div class="hydro-status p-4">
    <h3 class="text-lg font-bold mb-4">水文状态监测</h3>
    
    <!-- 当前水文状态卡片 -->
    <div class="grid grid-cols-2 gap-4 mb-6">
      <div class="bg-gradient-to-br from-blue-500 to-blue-700 rounded-lg p-6 text-white">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-4xl">🌊</span>
          <div>
            <div class="text-sm opacity-90">当前水位</div>
            <div class="text-4xl font-bold">{{ hydroData?.waterLevel.toFixed(2) || '--' }} m</div>
          </div>
        </div>
        <div class="flex items-center justify-between">
          <el-tag :type="getAlertTagType(hydroData?.alertLevel)" effect="dark">
            {{ getAlertText(hydroData?.alertLevel) }}
          </el-tag>
          <div class="text-sm opacity-80">
            更新: {{ formatTime(hydroData?.updateTime) }}
          </div>
        </div>
      </div>
      
      <div class="bg-gradient-to-br from-cyan-500 to-cyan-700 rounded-lg p-6 text-white">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-4xl">💨</span>
          <div>
            <div class="text-sm opacity-90">当前流量</div>
            <div class="text-4xl font-bold">{{ hydroData?.flowRate.toFixed(2) || '--' }} m³/s</div>
          </div>
        </div>
        <div class="text-sm opacity-80">
          相当于每秒通过 {{ (hydroData?.flowRate || 0).toFixed(0) }} 立方米水体
        </div>
      </div>
    </div>

    <!-- 水位警戒线 -->
    <el-card class="mb-4">
      <template #header>
        <span class="font-bold">水位警戒线</span>
      </template>
      <div class="mb-4">
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm text-gray-600">警戒线范围</span>
          <span class="text-sm font-semibold">0 - 25 m</span>
        </div>
        <el-progress 
          :percentage="getWaterLevelPercentage(hydroData?.waterLevel || 0)" 
          :status="getProgressStatus(hydroData?.waterLevel || 0)"
          :stroke-width="20"
          :show-text="true"
        />
        <div class="flex justify-between mt-2 text-xs text-gray-500">
          <span>0m</span>
          <span>5m (关注)</span>
          <span>15m (警告)</span>
          <span>20m (严重)</span>
          <span>25m (紧急)</span>
        </div>
      </div>
      
      <!-- 警戒指标 -->
      <div class="grid grid-cols-4 gap-4">
        <div class="text-center">
          <div class="text-2xl mb-1">🟢</div>
          <div class="text-sm text-gray-600">正常</div>
          <div class="text-xs text-gray-400">0-15m</div>
        </div>
        <div class="text-center">
          <div class="text-2xl mb-1">🟡</div>
          <div class="text-sm text-gray-600">关注</div>
          <div class="text-xs text-gray-400">15-18m</div>
        </div>
        <div class="text-center">
          <div class="text-2xl mb-1">🟠</div>
          <div class="text-sm text-gray-600">警告</div>
          <div class="text-xs text-gray-400">18-22m</div>
        </div>
        <div class="text-center">
          <div class="text-2xl mb-1">🔴</div>
          <div class="text-sm text-gray-600">紧急</div>
          <div class="text-xs text-gray-400">&gt;22m</div>
        </div>
      </div>
    </el-card>

    <!-- 水位变化曲线 -->
    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold">24小时水位变化</span>
          <el-button-group size="small">
            <el-button @click="showChart = 'waterLevel'">水位</el-button>
            <el-button @click="showChart = 'flowRate'">流量</el-button>
          </el-button-group>
        </div>
      </template>
      <div ref="chartRef" style="width: 100%; height: 300px;"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import type { HydroData, AlertLevel } from '@/types'

const props = defineProps<{
  plantId: string
  hydroData?: HydroData | null
}>()

const chartRef = ref<HTMLElement | null>(null)
const showChart = ref<'waterLevel' | 'flowRate'>('waterLevel')
let chartInstance: echarts.ECharts | null = null

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '--'
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取警戒标签类型
const getAlertTagType = (level?: AlertLevel) => {
  const types: Record<AlertLevel, string> = {
    normal: 'success',
    watch: 'primary',
    warning: 'warning',
    flood: 'danger'
  }
  return types[level || 'normal']
}

// 获取警戒文本
const getAlertText = (level?: AlertLevel) => {
  const texts: Record<AlertLevel, string> = {
    normal: '正常',
    watch: '关注',
    warning: '警告',
    flood: '洪水'
  }
  return texts[level || 'normal']
}

// 获取水位百分比
const getWaterLevelPercentage = (level: number) => {
  return Math.min(100, (level / 25) * 100)
}

// 获取进度条状态
const getProgressStatus = (level: number) => {
  if (level >= 22) return 'exception'
  if (level >= 18) return 'warning'
  return 'success'
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value || !props.hydroData?.history) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)

  const history = props.hydroData.history
  const times = history.map(item => {
    const date = new Date(item.time)
    return `${date.getHours()}:00`
  })

  const waterLevels = history.map(item => item.waterLevel)
  const flowRates = history.map(item => item.flowRate)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['水位', '流量'],
      top: 0
    },
    xAxis: {
      type: 'category',
      data: times,
      boundaryGap: false,
      axisLabel: {
        color: '#666'
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '水位 (m)',
        position: 'left',
        axisLabel: {
          color: '#666',
          formatter: '{value} m'
        }
      },
      {
        type: 'value',
        name: '流量 (m³/s)',
        position: 'right',
        axisLabel: {
          color: '#666',
          formatter: '{value}'
        }
      }
    ],
    series: [
      {
        name: '水位',
        type: 'line',
        smooth: true,
        data: waterLevels,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14, 165, 233, 0.5)' },
            { offset: 1, color: 'rgba(14, 165, 233, 0.1)' }
          ])
        },
        lineStyle: {
          color: '#0ea5e9',
          width: 2
        },
        itemStyle: {
          color: '#0ea5e9'
        }
      },
      {
        name: '流量',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: flowRates,
        lineStyle: {
          color: '#10b981',
          width: 2
        },
        itemStyle: {
          color: '#10b981'
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

  chartInstance.setOption(option)
}

// 监听数据和图表类型变化
watch([() => props.hydroData, showChart], () => {
  nextTick(() => {
    initChart()
  })
}, { deep: true })

onMounted(() => {
  nextTick(() => {
    initChart()
  })

  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})
</script>

<style scoped>
.hydro-status {
  position: relative;
}

.grid {
  display: grid;
}
</style>
