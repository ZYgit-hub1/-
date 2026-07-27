<template>
  <div class="weather-overview p-4" ref="containerRef">
    <!-- 雨效 Canvas 背景 -->
    <canvas 
      ref="rainCanvasRef" 
      class="rain-canvas"
      :class="{ active: showRainEffect, heavy: isHeavyRain }"
    ></canvas>

    <!-- 雨效指示器 -->
    <Transition name="fade">
      <div v-if="isHeavyRain" class="rain-indicator">
        <span class="indicator-icon">⛈️</span>
        <span class="indicator-text">暴雨预警</span>
        <span class="indicator-value">{{ weather?.rainfall }} mm/h</span>
      </div>
    </Transition>

    <h3 class="text-lg font-bold mb-4">气象信息概览</h3>
    
    <!-- 气象卡片 -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-6">
      <div class="bg-gradient-to-br from-blue-400 to-blue-600 rounded-lg p-4 text-white">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-2xl">🌡️</span>
          <span class="text-sm opacity-90">温度</span>
        </div>
        <div class="text-3xl font-bold">{{ weather?.temp || '--' }}°C</div>
      </div>
      
      <div class="bg-gradient-to-br from-cyan-400 to-cyan-600 rounded-lg p-4 text-white">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-2xl">💧</span>
          <span class="text-sm opacity-90">湿度</span>
        </div>
        <div class="text-3xl font-bold">{{ weather?.humidity || '--' }}%</div>
      </div>
      
      <div class="bg-gradient-to-br from-gray-400 to-gray-600 rounded-lg p-4 text-white">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-2xl">🌬️</span>
          <span class="text-sm opacity-90">风速</span>
        </div>
        <div class="text-3xl font-bold">{{ weather?.windSpeed || '--' }} m/s</div>
      </div>
      
      <div class="bg-gradient-to-br from-purple-400 to-purple-600 rounded-lg p-4 text-white">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-2xl">🧭</span>
          <span class="text-sm opacity-90">风向</span>
        </div>
        <div class="text-3xl font-bold">{{ weather?.windDirection || '--' }}°</div>
      </div>
      
      <div class="bg-gradient-to-br from-blue-300 to-blue-500 rounded-lg p-4 text-white relative overflow-hidden">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-2xl">🌧️</span>
          <span class="text-sm opacity-90">降雨</span>
        </div>
        <div class="text-3xl font-bold">{{ weather?.rainfall || '--' }} mm</div>
        <!-- 卡片内雨滴效果 -->
        <div v-if="isHeavyRain" class="card-rain-effect">
          <div v-for="i in 8" :key="i" class="card-rain-drop" :style="getCardRainStyle(i)"></div>
        </div>
      </div>
      
      <div class="bg-gradient-to-br from-indigo-400 to-indigo-600 rounded-lg p-4 text-white">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-2xl">📊</span>
          <span class="text-sm opacity-90">气压</span>
        </div>
        <div class="text-3xl font-bold">{{ weather?.pressure || '--' }} hPa</div>
      </div>
    </div>

    <!-- 24小时趋势图 -->
    <el-card class="mb-4 relative">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold">24小时温度趋势</span>
          <el-tag size="small" type="info">更新时间: {{ weather?.updateTime || '--' }}</el-tag>
        </div>
      </template>
      <div ref="tempChartRef" style="width: 100%; height: 300px;"></div>
    </el-card>

    <!-- 24小时预报 -->
    <el-card>
      <template #header>
        <span class="font-bold">24小时天气预报</span>
      </template>
      <div class="grid grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-4">
        <div 
          v-for="(item, index) in weather?.forecast" 
          :key="index"
          class="text-center p-2 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors"
        >
          <div class="text-sm text-gray-600 mb-2">{{ formatTime(item.time) }}</div>
          <div class="text-2xl mb-2">{{ getWeatherEmoji(item.weather) }}</div>
          <div class="text-lg font-bold">{{ item.temp }}°</div>
          <div class="text-xs text-gray-500">{{ item.weather }}</div>
          <div class="text-xs text-gray-500 mt-1">风速: {{ item.windSpeed }}m/s</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { WeatherData } from '@/types'

const props = defineProps<{
  plantId: string
  weather?: WeatherData | null
}>()

const containerRef = ref<HTMLElement | null>(null)
const tempChartRef = ref<HTMLElement | null>(null)
const rainCanvasRef = ref<HTMLCanvasElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// Canvas 雨效相关
let rainAnimationId: number | null = null
let raindrops: Raindrop[] = []

interface Raindrop {
  x: number
  y: number
  length: number
  speed: number
  opacity: number
  width: number
}

// 计算属性
const isHeavyRain = computed(() => (props.weather?.rainfall || 0) > 10)
const showRainEffect = computed(() => (props.weather?.rainfall || 0) > 5)

// 根据降雨量计算雨滴数量
const getRaindropCount = computed(() => {
  const rainfall = props.weather?.rainfall || 0
  if (rainfall > 20) return 200
  if (rainfall > 15) return 150
  if (rainfall > 10) return 100
  if (rainfall > 5) return 50
  return 0
})

// 获取卡片雨滴样式
function getCardRainStyle(index: number) {
  const delay = (index * 0.15) % 1
  const left = 10 + (index * 12) % 80
  return {
    left: `${left}%`,
    animationDelay: `${delay}s`
  }
}

// 初始化 Canvas 雨效
function initRainCanvas() {
  const canvas = rainCanvasRef.value
  const container = containerRef.value
  if (!canvas || !container) return

  // 设置 Canvas 尺寸
  const updateSize = () => {
    canvas.width = container.offsetWidth
    canvas.height = container.offsetHeight
  }
  updateSize()

  // 初始化雨滴
  function createRaindrops() {
    const count = getRaindropCount.value
    raindrops = []
    for (let i = 0; i < count; i++) {
      raindrops.push(createRaindrop())
    }
  }

  function createRaindrop(): Raindrop {
    const canvas = rainCanvasRef.value
    if (!canvas) return { x: 0, y: 0, length: 0, speed: 0, opacity: 0, width: 0 }
    
    const rainfall = props.weather?.rainfall || 0
    // 雨滴长度和速度随降雨量增加
    const lengthMultiplier = isHeavyRain.value ? 1.5 : 1
    const speedMultiplier = isHeavyRain.value ? 1.5 : 1
    
    return {
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height - canvas.height,
      length: (10 + Math.random() * 15) * lengthMultiplier,
      speed: (8 + Math.random() * 8) * speedMultiplier,
      opacity: 0.3 + Math.random() * 0.4,
      width: isHeavyRain.value ? 1.5 + Math.random() * 1 : 1 + Math.random() * 1
    }
  }

  // 动画循环
  function animateRain() {
    const canvas = rainCanvasRef.value
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // 清除画布（带轻微拖尾效果）
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    // 绘制雨滴
    raindrops.forEach((drop, index) => {
      // 渐变效果
      const gradient = ctx.createLinearGradient(
        drop.x, drop.y, 
        drop.x, drop.y + drop.length
      )
      
      // 暴雨时颜色更深
      if (isHeavyRain.value) {
        gradient.addColorStop(0, 'rgba(100, 149, 237, 0)')
        gradient.addColorStop(1, 'rgba(100, 149, 237, 0.7)')
      } else {
        gradient.addColorStop(0, 'rgba(135, 206, 235, 0)')
        gradient.addColorStop(1, `rgba(135, 206, 235, ${drop.opacity})`)
      }
      
      ctx.beginPath()
      ctx.strokeStyle = gradient
      ctx.lineWidth = drop.width
      ctx.lineCap = 'round'
      ctx.moveTo(drop.x, drop.y)
      ctx.lineTo(drop.x + 1, drop.y + drop.length)
      ctx.stroke()

      // 更新位置
      drop.y += drop.speed
      drop.x += 1 // 稍微倾斜

      // 重置雨滴
      if (drop.y > canvas.height) {
        raindrops[index] = createRaindrop()
        raindrops[index].y = -drop.length
      }
    })

    // 暴雨时添加溅射效果
    if (isHeavyRain.value) {
      drawSplash(ctx)
    }

    rainAnimationId = requestAnimationFrame(animateRain)
  }

  // 溅射效果
  function drawSplash(ctx: CanvasRenderingContext2D) {
    const canvas = rainCanvasRef.value
    if (!canvas) return
    
    for (let i = 0; i < 5; i++) {
      const x = Math.random() * canvas.width
      const y = canvas.height - Math.random() * 50
      
      ctx.beginPath()
      ctx.fillStyle = 'rgba(135, 206, 235, 0.3)'
      
      // 小圆点模拟溅射
      for (let j = 0; j < 3; j++) {
        const offsetX = (Math.random() - 0.5) * 20
        ctx.moveTo(x + offsetX, y)
        ctx.arc(x + offsetX, y + Math.random() * 5, 1, 0, Math.PI * 2)
      }
      ctx.fill()
    }
  }

  // 根据降雨量更新雨滴数量
  watch(getRaindropCount, (newCount) => {
    while (raindrops.length < newCount) {
      raindrops.push(createRaindrop())
    }
    while (raindrops.length > newCount) {
      raindrops.pop()
    }
  })

  createRaindrops()
  animateRain()
}

// 停止雨效动画
function stopRainAnimation() {
  if (rainAnimationId !== null) {
    cancelAnimationFrame(rainAnimationId)
    rainAnimationId = null
  }
}

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  return `${date.getHours()}:00`
}

// 获取天气emoji
const getWeatherEmoji = (weather: string) => {
  const emojis: Record<string, string> = {
    '晴': '☀️',
    '多云': '⛅',
    '阴': '☁️',
    '小雨': '🌧️',
    '阵雨': '⛈️'
  }
  return emojis[weather] || '🌤️'
}

// 初始化图表
const initChart = () => {
  if (!tempChartRef.value || !props.weather?.forecast) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(tempChartRef.value)

  const forecast = props.weather.forecast
  const times = forecast.map(item => formatTime(item.time))
  const temps = forecast.map(item => item.temp)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>温度: {c}°C'
    },
    xAxis: {
      type: 'category',
      data: times,
      boundaryGap: false,
      axisLabel: {
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#666',
        formatter: '{value}°C'
      }
    },
    series: [
      {
        name: '温度',
        type: 'line',
        smooth: true,
        data: temps,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14, 165, 233, 0.5)' },
            { offset: 1, color: 'rgba(14, 165, 233, 0.1)' }
          ])
        },
        lineStyle: {
          color: '#0ea5e9',
          width: 3
        },
        itemStyle: {
          color: '#0ea5e9'
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

// 监听数据变化
watch(() => props.weather, () => {
  nextTick(() => {
    initChart()
  })
}, { deep: true })

// 监听容器大小变化
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(() => {
    initChart()
    initRainCanvas()
  })

  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })

  // 监听容器大小变化
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      if (rainCanvasRef.value && containerRef.value) {
        rainCanvasRef.value.width = containerRef.value.offsetWidth
        rainCanvasRef.value.height = containerRef.value.offsetHeight
      }
    })
    resizeObserver.observe(containerRef.value)
  }
})

onUnmounted(() => {
  stopRainAnimation()
  chartInstance?.dispose()
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.weather-overview {
  position: relative;
  overflow: hidden;
}

.grid {
  display: grid;
}

/* Canvas 雨效 */
.rain-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  opacity: 0;
  transition: opacity 0.5s ease;
}

.rain-canvas.active {
  opacity: 1;
}

.rain-canvas.heavy {
  opacity: 1;
}

/* 暴雨时的背景效果 */
.rain-canvas.heavy::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 0%, rgba(100, 149, 237, 0.1) 0%, transparent 70%);
  pointer-events: none;
}

/* 雨效指示器 */
.rain-indicator {
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 10;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.02); }
}

.indicator-icon {
  font-size: 20px;
}

.indicator-text {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.indicator-value {
  padding: 4px 10px;
  background: #dc2626;
  color: white;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

/* 卡片内雨滴效果 */
.card-rain-effect {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
}

.card-rain-drop {
  position: absolute;
  top: -20px;
  width: 2px;
  height: 15px;
  background: linear-gradient(to bottom, transparent, rgba(255, 255, 255, 0.6));
  animation: card-rain-fall 0.8s linear infinite;
}

@keyframes card-rain-fall {
  0% {
    top: -20px;
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    top: 100%;
    opacity: 0;
  }
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
