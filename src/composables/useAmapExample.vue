/**
 * 高德地图使用示例
 * 
 * 本文件展示 useAmap composable 的各种使用方式
 */

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAmap, type AddMarkerOptions, WARNING_COLORS, PLANT_ICONS } from './useAmap'
import type { PowerPlant, MapMarker } from '@/types'

// 引入类型
interface PowerPlant {
  id: string
  name: string
  type: 'coal' | 'gas' | 'solar' | 'wind' | 'storage'
  location: { lat: number; lng: number }
  warningLevel?: 'green' | 'blue' | 'yellow' | 'orange' | 'red'
}

const mapContainer = ref<HTMLElement | null>(null)
const selectedMarker = ref<MapMarker | null>(null)

// 初始化地图
const {
  map,
  isLoaded,
  error,
  markers,
  initMap,
  addMarker,
  addMarkers,
  removeMarker,
  clearMarkers,
  setMarkers,
  openInfoWindow,
  closeInfoWindow,
  onClick,
  onMarkerClick,
  onZoomChange,
  setCenter,
  setZoom,
  panTo,
  setFitView,
  destroy
} = useAmap()

// 示例数据
const plants: PowerPlant[] = [
  { id: '1', name: '广州电厂', type: 'coal', location: { lat: 23.125178, lng: 113.280637 }, warningLevel: 'green' },
  { id: '2', name: '深圳能源中心', type: 'gas', location: { lat: 22.543099, lng: 114.057868 }, warningLevel: 'blue' },
  { id: '3', name: '珠海光伏电站', type: 'solar', location: { lat: 22.271110, lng: 113.576726 }, warningLevel: 'yellow' },
  { id: '4', name: '汕头风电场', type: 'wind', location: { lat: 23.353501, lng: 116.682037 }, warningLevel: 'orange' },
  { id: '5', name: '东莞储能站', type: 'storage', location: { lat: 23.046932, lng: 113.744632 }, warningLevel: 'red' }
]

onMounted(async () => {
  // 初始化地图
  await initMap('amap-container', {
    zoom: 8,
    center: [113.280637, 23.125178],
    viewMode: '2D'
  })

  // 添加标记点
  addPlantMarkers()

  // 监听地图点击
  onClick((e) => {
    console.log('地图点击:', e.lnglat.lng, e.lnglat.lat)
    closeInfoWindow()
  })

  // 监听标记点击
  onMarkerClick((marker, e) => {
    selectedMarker.value = marker
    const data = marker.data as PowerPlant
    const infoContent = createInfoWindowContent(data)
    openInfoWindow(infoContent, [data.location.lng, data.location.lat])
  })

  // 监听缩放变化
  onZoomChange((zoom) => {
    console.log('当前缩放级别:', zoom)
  })
})

// 添加电厂标记
function addPlantMarkers() {
  const markerOptions: AddMarkerOptions[] = plants.map(plant => ({
    id: plant.id,
    position: [plant.location.lng, plant.location.lat],
    title: plant.name,
    content: createMarkerContent(plant),
    extData: plant,
    clickable: true,
    animation: 'AMAP_ANIMATION_DROP'
  }))

  addMarkers(markerOptions)
}

// 创建标记内容
function createMarkerContent(plant: PowerPlant): string {
  const color = WARNING_COLORS[plant.warningLevel || 'green']
  const icons: Record<string, string> = {
    coal: '🏭',
    gas: '⚡',
    solar: '☀️',
    wind: '🌬️',
    storage: '🔋'
  }
  const icon = icons[plant.type] || '🏭'

  return `
    <div style="
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      background: ${color};
      border: 3px solid white;
      border-radius: 50%;
      box-shadow: 0 4px 12px rgba(0,0,0,0.3);
      font-size: 18px;
    ">
      ${icon}
    </div>
  `
}

// 创建信息窗口内容
function createInfoWindowContent(plant: PowerPlant): string {
  const typeNames: Record<string, string> = {
    coal: '火电厂',
    gas: '燃气电厂',
    solar: '光伏电站',
    wind: '风电场',
    storage: '储能站'
  }
  const levelNames: Record<string, string> = {
    green: '正常',
    blue: '关注',
    yellow: '警告',
    orange: '严重',
    red: '危险'
  }

  return `
    <div style="padding: 12px; min-width: 200px; font-family: -apple-system, sans-serif;">
      <h3 style="margin: 0 0 8px 0; font-size: 16px; color: #1f2937;">${plant.name}</h3>
      <div style="font-size: 13px; color: #6b7280; line-height: 1.6;">
        <div><strong>类型：</strong>${typeNames[plant.type]}</div>
        <div><strong>状态：</strong><span style="color: ${WARNING_COLORS[plant.warningLevel || 'green']}">${levelNames[plant.warningLevel || 'green']}</span></div>
        <div><strong>坐标：</strong>${plant.location.lng.toFixed(4)}, ${plant.location.lat.toFixed(4)}</div>
      </div>
      <button 
        onclick="window.dispatchEvent(new CustomEvent('view-plant-detail', {detail: '${plant.id}'}))"
        style="
          margin-top: 12px;
          width: 100%;
          padding: 8px 16px;
          background: #0ea5e9;
          color: white;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          font-size: 13px;
        "
      >
        查看详情
      </button>
    </div>
  `
}

// 跳转到指定电厂
function goToPlant(plantId: string) {
  const plant = plants.find(p => p.id === plantId)
  if (plant) {
    panTo([plant.location.lng, plant.location.lat])
    setZoom(14)
  }
}

// 聚焦所有电厂
function fitAllPlants() {
  setFitView(50)
}
</script>

<template>
  <div class="map-demo">
    <!-- 地图容器 -->
    <div 
      id="amap-container" 
      ref="mapContainer"
      style="width: 100%; height: 500px; border-radius: 8px; overflow: hidden;"
    />

    <!-- 加载状态 -->
    <div v-if="!isLoaded && !error" class="loading">
      地图加载中...
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error">
      {{ error }}
    </div>

    <!-- 控制面板 -->
    <div class="controls">
      <h3>地图控制</h3>
      <button @click="fitAllPlants">显示全部电厂</button>
      <button @click="setZoom(10)">缩放到10级</button>
      <button @click="setZoom(14)">缩放到14级</button>
      <button @click="clearMarkers">清空标记</button>
      <button @click="addPlantMarkers">重新加载</button>
      
      <h3>快速定位</h3>
      <button 
        v-for="plant in plants" 
        :key="plant.id"
        @click="goToPlant(plant.id)"
      >
        {{ plant.name }}
      </button>
    </div>

    <!-- 标记列表 -->
    <div class="marker-list">
      <h3>当前标记 ({{ markers.length }})</h3>
      <div v-for="m in markers" :key="m.id" class="marker-item">
        <span>{{ m.data?.name }}</span>
        <span>{{ m.data?.type }}</span>
        <button @click="removeMarker(m.id)">删除</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.map-demo {
  padding: 20px;
}

.loading {
  padding: 20px;
  text-align: center;
  color: #666;
}

.error {
  padding: 20px;
  text-align: center;
  color: #ef4444;
  background: #fef2f2;
  border-radius: 8px;
}

.controls {
  margin-top: 20px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.controls h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #374151;
}

.controls button {
  margin: 4px;
  padding: 8px 12px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}

.controls button:hover {
  background: #f3f4f6;
}

.marker-list {
  margin-top: 20px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.marker-list h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #374151;
}

.marker-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background: white;
  border-radius: 4px;
  margin-bottom: 4px;
}

.marker-item button {
  padding: 4px 8px;
  font-size: 12px;
  color: #ef4444;
  background: #fef2f2;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>
