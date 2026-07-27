<template>
  <div class="real-time-monitor p-4">
    <h3 class="text-lg font-bold mb-4">实时监测数据</h3>
    
    <!-- 实时数据卡片 -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="bg-gradient-to-br from-green-400 to-green-600 rounded-lg p-4 text-white">
        <div class="flex items-center justify-between mb-2">
          <span class="text-2xl">⚡</span>
          <el-tag size="small" type="success" effect="dark">正常</el-tag>
        </div>
        <div class="text-sm opacity-90 mb-1">发电功率</div>
        <div class="text-2xl font-bold">{{ powerOutput }} MW</div>
        <div class="text-xs opacity-75 mt-1">实时更新</div>
      </div>
      
      <div class="bg-gradient-to-br from-blue-400 to-blue-600 rounded-lg p-4 text-white">
        <div class="flex items-center justify-between mb-2">
          <span class="text-2xl">🔋</span>
          <el-tag size="small" type="primary" effect="dark">运行中</el-tag>
        </div>
        <div class="text-sm opacity-90 mb-1">SOC 容量</div>
        <div class="text-2xl font-bold">{{ socCapacity }}%</div>
        <div class="text-xs opacity-75 mt-1">电池状态</div>
      </div>
      
      <div class="bg-gradient-to-br from-yellow-400 to-orange-500 rounded-lg p-4 text-white">
        <div class="flex items-center justify-between mb-2">
          <span class="text-2xl">🌡️</span>
          <el-tag size="small" type="warning" effect="dark">监测中</el-tag>
        </div>
        <div class="text-sm opacity-90 mb-1">设备温度</div>
        <div class="text-2xl font-bold">{{ deviceTemp }}°C</div>
        <div class="text-xs opacity-75 mt-1">最高温度</div>
      </div>
      
      <div class="bg-gradient-to-br from-purple-400 to-purple-600 rounded-lg p-4 text-white">
        <div class="flex items-center justify-between mb-2">
          <span class="text-2xl">⏱️</span>
          <el-tag size="small" type="info" effect="dark">在线</el-tag>
        </div>
        <div class="text-sm opacity-90 mb-1">运行时间</div>
        <div class="text-2xl font-bold">{{ runTime }}h</div>
        <div class="text-xs opacity-75 mt-1">连续运行</div>
      </div>
    </div>

    <!-- 实时曲线 -->
    <el-card class="mb-4">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold">发电功率实时曲线</span>
          <el-tag size="small" type="success">
            <el-icon class="is-loading"><Loading /></el-icon>
            实时更新中
          </el-tag>
        </div>
      </template>
      <div ref="chartRef" style="width: 100%; height: 300px;"></div>
    </el-card>

    <!-- 设备状态列表 -->
    <el-card>
      <template #header>
        <span class="font-bold">设备状态监控</span>
      </template>
      <el-table :data="deviceList" stripe style="width: 100%">
        <el-table-column prop="name" label="设备名称" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getDeviceStatusType(row.status)" size="small">
              {{ getDeviceStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="当前值" width="120" />
        <el-table-column prop="threshold" label="阈值" width="120" />
        <el-table-column prop="updateTime" label="更新时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

defineProps<{
  plantId: string
}>()

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let updateTimer: number | null = null

const powerOutput = ref(0)
const socCapacity = ref(0)
const deviceTemp = ref(0)
const runTime = ref(0)

const deviceList = ref([
  { name: '主机组 A', status: 'normal', value: '450 MW', threshold: '500 MW', updateTime: '2分钟前' },
  { name: '主机组 B', status: 'normal', value: '380 MW', threshold: '500 MW', updateTime: '2分钟前' },
  { name: '变压器 1', status: 'normal', value: '35°C', threshold: '80°C', updateTime: '3分钟前' },
  { name: '变压器 2', status: 'warning', value: '68°C', threshold: '80°C', updateTime: '3分钟前' },
  { name: '冷却系统', status: 'normal', value: '正常', threshold: '-', updateTime: '1分钟前' },
  { name: '消防系统', status: 'normal', value: '正常', threshold: '-', updateTime: '5分钟前' }
])

const timeData = ref<string[]>([])
const powerData = ref<number[]>([])

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>功率: {c} MW'
    },
    xAxis: {
      type: 'category',
      data: timeData.value,
      boundaryGap: false,
      axisLabel: {
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#666',
        formatter: '{value} MW'
      }
    },
    series: [
      {
        name: '发电功率',
        type: 'line',
        smooth: true,
        data: powerData.value,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.5)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.1)' }
          ])
        },
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

// 更新数据
const updateData = () => {
  const now = new Date()
  const time = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  
  timeData.value.push(time)
  powerData.value.push(Math.random() * 200 + 300)
  
  // 保持最近30个数据点
  if (timeData.value.length > 30) {
    timeData.value.shift()
    powerData.value.shift()
  }
  
  // 更新实时数据
  powerOutput.value = Math.floor(Math.random() * 100 + 350)
  socCapacity.value = Math.floor(Math.random() * 30 + 60)
  deviceTemp.value = Math.floor(Math.random() * 15 + 50)
  runTime.value = Math.floor(Math.random() * 100 + 500)

  // 更新图表
  chartInstance?.setOption({
    xAxis: {
      data: timeData.value
    },
    series: [{
      data: powerData.value
    }]
  })
}

// 获取设备状态类型
const getDeviceStatusType = (status: string) => {
  const types: Record<string, string> = {
    normal: 'success',
    warning: 'warning',
    error: 'danger'
  }
  return types[status] || 'info'
}

// 获取设备状态文本
const getDeviceStatusText = (status: string) => {
  const texts: Record<string, string> = {
    normal: '正常',
    warning: '警告',
    error: '故障'
  }
  return texts[status] || status
}

onMounted(() => {
  // 初始化图表
  initChart()
  
  // 启动定时更新
  updateTimer = window.setInterval(updateData, 3000)
  
  // 窗口大小变化时重新调整图表
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})

onUnmounted(() => {
  if (updateTimer) {
    clearInterval(updateTimer)
  }
  if (chartInstance) {
    chartInstance.dispose()
  }
})
</script>

<style scoped>
.real-time-monitor {
  position: relative;
}

.grid {
  display: grid;
}
</style>
