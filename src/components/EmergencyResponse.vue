<template>
  <div class="emergency-response p-4">
    <h3 class="text-lg font-bold mb-4">应急响应预案</h3>
    
    <!-- 应急响应状态 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
      <div class="bg-gradient-to-br from-orange-500 to-red-600 rounded-lg p-6 text-white">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-4xl">🚨</span>
          <div>
            <div class="text-sm opacity-90">预案等级</div>
            <div class="text-3xl font-bold">{{ emergency?.planLevel || '三级响应' }}</div>
          </div>
        </div>
        <el-tag type="danger" effect="dark" size="large">
          {{ getStatusText(emergency?.status) }}
        </el-tag>
      </div>
      
      <div class="bg-white rounded-lg p-6 shadow">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-4xl">👤</span>
          <div>
            <div class="text-sm text-gray-600">现场指挥</div>
            <div class="text-2xl font-bold text-gray-800">{{ emergency?.commander || '--' }}</div>
          </div>
        </div>
      </div>
      
      <div class="bg-white rounded-lg p-6 shadow">
        <div class="flex items-center gap-3 mb-4">
          <span class="text-4xl">📞</span>
          <div>
            <div class="text-sm text-gray-600">联系电话</div>
            <div class="text-2xl font-bold text-gray-800">{{ emergency?.contactPhone || '--' }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 应急措施 -->
    <el-card class="mb-4">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold">应急响应措施</span>
          <el-tag v-if="emergency?.status === 'activated'" type="danger" effect="dark">
            已启动
          </el-tag>
        </div>
      </template>
      
      <div class="space-y-4">
        <div 
          v-for="(measure, index) in emergency?.measures" 
          :key="index"
          class="flex items-start gap-3 p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors"
        >
          <div 
            class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center"
            :class="{
              'bg-orange-100 text-orange-600': emergency?.status === 'activated',
              'bg-gray-200 text-gray-600': emergency?.status !== 'activated'
            }"
          >
            <el-icon v-if="emergency?.status === 'activated'">
              <Check />
            </el-icon>
            <span v-else>{{ index + 1 }}</span>
          </div>
          <div class="flex-1">
            <div class="font-semibold text-gray-800">{{ measure }}</div>
            <div class="text-sm text-gray-500 mt-1">
              {{ emergency?.status === 'activated' ? '已执行' : '待执行' }}
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 应急预案详情 -->
    <el-card>
      <template #header>
        <span class="font-bold">应急预案说明</span>
      </template>
      
      <el-collapse v-model="activeNames">
        <el-collapse-item title="一级响应标准" name="1">
          <div class="text-gray-600">
            <p>当发生以下情况时，启动一级响应：</p>
            <ul class="list-disc list-inside mt-2 space-y-1">
              <li>台风红色预警</li>
              <li>洪水红色预警</li>
              <li>电厂发生重大安全事故</li>
              <li>多个电厂同时告警</li>
            </ul>
          </div>
        </el-collapse-item>
        
        <el-collapse-item title="二级响应标准" name="2">
          <div class="text-gray-600">
            <p>当发生以下情况时，启动二级响应：</p>
            <ul class="list-disc list-inside mt-2 space-y-1">
              <li>台风橙色预警</li>
              <li>洪水橙色预警</li>
              <li>电厂设备严重故障</li>
              <li>气象条件严重影响发电</li>
            </ul>
          </div>
        </el-collapse-item>
        
        <el-collapse-item title="三级响应标准" name="3">
          <div class="text-gray-600">
            <p>当发生以下情况时，启动三级响应：</p>
            <ul class="list-disc list-inside mt-2 space-y-1">
              <li>台风黄色/蓝色预警</li>
              <li>洪水黄色预警</li>
              <li>单个电厂告警</li>
              <li>设备预防性维护</li>
            </ul>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { EmergencyResponse } from '@/types'

defineProps<{
  plantId: string
  emergency?: EmergencyResponse | null
}>()

const activeNames = ref(['1', '2', '3'])

const getStatusText = (status?: string) => {
  const texts: Record<string, string> = {
    standby: '待命',
    activated: '已启动',
    ended: '已结束'
  }
  return texts[status || 'standby'] || status
}
</script>

<style scoped>
.emergency-response {
  position: relative;
}

.grid {
  display: grid;
}

.space-y-4 > * + * {
  margin-top: 1rem;
}

.space-y-1 > * + * {
  margin-top: 0.25rem;
}

.list-disc {
  list-style-type: disc;
}

.list-inside {
  list-style-position: inside;
}
</style>
