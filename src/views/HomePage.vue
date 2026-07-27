<template>
  <div class="home-page h-full flex flex-col">
    <!-- 顶部统计卡片 -->
    <div class="cyber-header text-white p-4 shadow-lg">
      <div class="container mx-auto">
        <div class="flex items-center justify-between mb-4">
          <h1 class="text-xl font-bold">广东省电厂监控平台</h1>
          <div class="flex items-center gap-4">
            <div class="flex items-center gap-2">
              <el-icon><Clock /></el-icon>
              <span class="text-sm">{{ currentTime }}</span>
            </div>
          </div>
        </div>
        
        <!-- 统计卡片 -->
        <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
          <div class="bg-white bg-opacity-20 rounded-lg p-3">
            <div class="text-2xl font-bold">{{ stats.totalPlants }}</div>
            <div class="text-xs opacity-80">电厂总数</div>
          </div>
          <div class="bg-green-500 bg-opacity-80 rounded-lg p-3">
            <div class="text-2xl font-bold">{{ stats.normalPlants }}</div>
            <div class="text-xs opacity-90">正常运行</div>
          </div>
          <div class="bg-yellow-500 bg-opacity-80 rounded-lg p-3">
            <div class="text-2xl font-bold">{{ stats.warningPlants }}</div>
            <div class="text-xs opacity-90">预警电厂</div>
          </div>
          <div class="bg-red-500 bg-opacity-80 rounded-lg p-3">
            <div class="text-2xl font-bold">{{ stats.dangerPlants }}</div>
            <div class="text-xs opacity-90">紧急告警</div>
          </div>
          <div class="bg-blue-500 bg-opacity-80 rounded-lg p-3">
            <div class="text-2xl font-bold">{{ stats.totalAlarms }}</div>
            <div class="text-xs opacity-90">报警总数</div>
          </div>
          <div class="bg-orange-500 bg-opacity-80 rounded-lg p-3">
            <div class="text-2xl font-bold">{{ stats.activeWarnings }}</div>
            <div class="text-xs opacity-90">活跃预警</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主地图区域 -->
    <div class="flex-1 relative">
      <PowerPlantMap 
        :plants="filteredPlants"
        @plant-click="handlePlantClick"
        @filter-change="handleFilterChange"
      />
    </div>

    <!-- 底部快捷入口 -->
    <div class="fixed bottom-4 right-4 flex flex-col gap-2 z-30">
      <el-button 
        type="danger" 
        circle 
        size="large"
        @click="$router.push('/alarm')"
        class="shadow-lg"
      >
        <el-badge :value="stats.unreadAlarms" :hidden="stats.unreadAlarms === 0">
          <el-icon><Bell /></el-icon>
        </el-badge>
      </el-button>
      <el-button 
        type="warning" 
        circle 
        size="large"
        @click="$router.push('/warning')"
        class="shadow-lg"
      >
        <el-icon><Warning /></el-icon>
      </el-button>
      <el-button 
        type="primary" 
        circle 
        size="large"
        @click="$router.push('/stats')"
        class="shadow-lg"
      >
        <el-icon><DataAnalysis /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PowerPlantMap from '@/components/PowerPlantMap.vue'
import { usePlants } from '@/composables/usePlants'
import { mockDataService } from '@/mock/data'
import type { PowerPlant, DashboardStats } from '@/types'

const router = useRouter()
const { filteredPlants, loadPlants } = usePlants()

const stats = ref<DashboardStats>({
  totalPlants: 0,
  normalPlants: 0,
  warningPlants: 0,
  dangerPlants: 0,
  totalAlarms: 0,
  unreadAlarms: 0,
  activeWarnings: 0
})

const currentTime = ref('')

// 更新当前时间
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 处理电厂点击
const handlePlantClick = (plant: PowerPlant) => {
  router.push(`/plant/${plant.id}`)
}

// 处理筛选变化
const handleFilterChange = (filters: { warningLevel: string; type: string }) => {
  console.log('筛选条件:', filters)
}

// 初始化
onMounted(async () => {
  await loadPlants()
  stats.value = mockDataService.getStats()
  updateTime()

  // 每秒更新时间
  const timer = setInterval(updateTime, 1000)

  onUnmounted(() => {
    clearInterval(timer)
  })
})
</script>

<style scoped>
.home-page {
  background: #07091a;
}

.cyber-header {
  background: linear-gradient(135deg, #0a1a3e 0%, #1a0a2e 50%, #0a0e27 100%);
  border-bottom: 1px solid rgba(0, 212, 255, 0.3);
  box-shadow: 0 0 24px rgba(0, 212, 255, 0.15), 0 4px 16px rgba(0, 0, 0, 0.5);
}

.container {
  max-width: 1400px;
}

.grid {
  display: grid;
}
</style>
