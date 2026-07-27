<template>
  <div class="power-plant-map">
    <!-- 地图容器（高德地图 / 占位 SVG 都会挂到这里） -->
    <div
      ref="mapContainerRef"
      id="power-plant-map-container"
      class="map-container"
      @click="closePopover"
    >
      <!-- 占位地图（无 Key 时启用，使用用户提供的广东省地图图片作为底图） -->
      <div v-if="isPlaceholder" class="placeholder-map" ref="placeholderMapRef" @wheel.prevent="handleWheelZoom">
        <div class="map-stage" ref="mapStageRef" :style="mapStageStyle">
          <img
            src="@/assets/guangdong-hydro-map.png"
            alt="广东省地图"
            class="map-image"
            draggable="false"
          />

          <!-- 城市参考标签（半透明叠层） -->
          <div class="city-labels">
            <span
              v-for="city in referenceCities"
              :key="city.name"
              class="city-label"
              :style="{ left: city.xPct + '%', top: city.yPct + '%' }"
            >{{ city.name }}</span>
          </div>

          <!-- 电厂点位（绝对定位，百分比随图片自适应） -->
          <div class="plant-points">
            <button
              v-for="pt in placeholderPoints"
              :key="pt.plant.id"
              type="button"
              class="plant-pin"
              :class="{ 'plant-pin--warning': pt.isWarning }"
              :style="{
                left: pt.xPct + '%',
                top: pt.yPct + '%',
                '--pin-warn-color': pt.color
              }"
              :title="pt.plant.name"
              @click.stop="handlePlaceholderClick(pt.plant)"
            >
              <span class="plant-pin-icon">{{ pt.emoji }}</span>
              <span class="plant-pin-pulse" />
            </button>
          </div>

          <!-- 水文站点位（带实时水位数据） -->
          <div class="hydro-points">
            <button
              v-for="pt in hydroPoints"
              :key="pt.station.id"
              type="button"
              class="hydro-pin"
              :class="{ 'hydro-pin--alert': pt.isAlert }"
              :style="{
                left: pt.xPct + '%',
                top: pt.yPct + '%',
                '--pin-color': pt.color
              }"
              :title="pt.station.name"
              @click.stop="handleHydroPinClick(pt.station)"
            >
              <!-- 小房子形锚点 -->
              <span class="hydro-pin-drop">
                <svg viewBox="0 0 24 24" width="7" height="7">
                  <path
                    d="M3 11 L12 3 L21 11 L21 20 a1 1 0 0 1 -1 1 H4 a1 1 0 0 1 -1 -1 Z M10 21 V14 H14 V21"
                    :fill="pt.color"
                    stroke="#ffffff"
                    stroke-width="1.6"
                    stroke-linejoin="round"
                  />
                </svg>
                <span class="hydro-pin-trend" :style="{ color: pt.color }">
                  {{ TREND_ICON[pt.reading?.trend ?? 'steady'] }}
                </span>
              </span>
              <!-- 水位数值气泡 -->
              <span class="hydro-pin-label" :style="{ borderColor: pt.color, color: pt.color }">
                {{ pt.reading ? pt.reading.waterLevel.toFixed(2) + 'm' : '--' }}
              </span>
              <span class="hydro-pin-pulse" v-if="pt.isAlert" />
            </button>
          </div>

          <!-- 水文站信息弹窗 -->
          <Transition name="info-fade">
            <div
              v-if="selectedHydroStation"
              class="hydro-popover"
              :class="{
                'flip-down': selectedHydroFlipDown,
                'shift-left': selectedHydroHShift === 'left',
                'shift-right': selectedHydroHShift === 'right'
              }"
              :style="{
                left: selectedHydroStationPos.xPct + '%',
                top: selectedHydroStationPos.yPct + '%'
              }"
              @click.stop
            >
              <button class="popover-close" @click="selectedHydroStation = null" aria-label="关闭">×</button>
              <div class="popover-header">
                <span class="popover-icon hydro-popover-icon">🏠</span>
                <div>
                  <h3 class="popover-title">{{ selectedHydroStation.name }}</h3>
                  <span class="popover-subtitle">{{ selectedHydroStation.river }} · {{ selectedHydroStation.city }}</span>
                </div>
              </div>
              <div
                class="popover-level"
                :style="{
                  color: HYDRO_ALERT_COLORS[selectedHydroStation.current?.alertLevel ?? 'normal'],
                  background: (HYDRO_ALERT_COLORS[selectedHydroStation.current?.alertLevel ?? 'normal']) + '15',
                  borderColor: (HYDRO_ALERT_COLORS[selectedHydroStation.current?.alertLevel ?? 'normal']) + '40'
                }"
              >
                {{ HYDRO_ALERT_NAMES[selectedHydroStation.current?.alertLevel ?? 'normal'] }}
                <span v-if="selectedHydroStation.current && selectedHydroStation.current.overWarning >= 0" class="popover-level-sub">
                  超警 {{ selectedHydroStation.current.overWarning.toFixed(2) }}m
                </span>
                <span v-else-if="selectedHydroStation.current" class="popover-level-sub">
                  低于警戒 {{ Math.abs(selectedHydroStation.current.overWarning).toFixed(2) }}m
                </span>
              </div>
              <div class="popover-rows">
                <div class="popover-row">
                  <span class="popover-row-label">实时水位</span>
                  <span class="popover-row-value hydro-value-main">
                    {{ selectedHydroStation.current?.waterLevel.toFixed(2) ?? '--' }} m
                  </span>
                </div>
                <div class="popover-row">
                  <span class="popover-row-label">水势</span>
                  <span class="popover-row-value">
                    {{ TREND_ICON[selectedHydroStation.current?.trend ?? 'steady'] }}
                    {{ TREND_NAME[selectedHydroStation.current?.trend ?? 'steady'] }}
                  </span>
                </div>
                <div class="popover-row">
                  <span class="popover-row-label">实时流量</span>
                  <span class="popover-row-value">{{ selectedHydroStation.current?.flowRate ?? '--' }} m³/s</span>
                </div>
                <div class="popover-row">
                  <span class="popover-row-label">警戒水位</span>
                  <span class="popover-row-value">{{ selectedHydroStation.warningLevel.toFixed(2) }} m</span>
                </div>
                <div class="popover-row">
                  <span class="popover-row-label">保证水位</span>
                  <span class="popover-row-value">{{ selectedHydroStation.guaranteeLevel.toFixed(2) }} m</span>
                </div>
                <div class="popover-row">
                  <span class="popover-row-label">历史最高</span>
                  <span class="popover-row-value">{{ selectedHydroStation.historicalMax.toFixed(2) }} m</span>
                </div>
              </div>
              <!-- 水位条形可视化 -->
              <div class="hydro-bar" v-if="selectedHydroStation.current">
                <div class="hydro-bar-track">
                  <div class="hydro-bar-mark hydro-bar-warn" :style="{ left: ((selectedHydroStation.warningLevel / selectedHydroStation.guaranteeLevel) * 100) + '%' }" />
                  <div class="hydro-bar-mark hydro-bar-guar" :style="{ left: '100%' }" />
                  <div
                    class="hydro-bar-fill"
                    :style="{
                      width: Math.min(100, (selectedHydroStation.current.waterLevel / selectedHydroStation.guaranteeLevel) * 100) + '%',
                      background: HYDRO_ALERT_COLORS[selectedHydroStation.current.alertLevel]
                    }"
                  />
                </div>
                <div class="hydro-bar-legend">
                  <span>0</span>
                  <span>警戒 {{ selectedHydroStation.warningLevel.toFixed(1) }}m</span>
                  <span>保证 {{ selectedHydroStation.guaranteeLevel.toFixed(1) }}m</span>
                </div>
              </div>
              <span class="popover-arrow" />
            </div>
          </Transition>

          <!-- HTML 信息弹窗（替代 AMap InfoWindow） -->
          <Transition name="info-fade">
            <div
              v-if="selectedPlant"
              class="plant-popover"
              :class="{
                'flip-down': selectedPlantFlipDown,
                'shift-left': selectedPlantHShift === 'left',
                'shift-right': selectedPlantHShift === 'right'
              }"
              :style="{
                left: selectedPlantPos.xPct + '%',
                top: selectedPlantPos.yPct + '%'
              }"
              @click.stop
            >
              <button class="popover-close" @click="selectedPlant = null" aria-label="关闭">×</button>
              <div class="popover-header">
                <span class="popover-icon">{{ selectedPlantMeta.icon }}</span>
                <div>
                  <h3 class="popover-title">{{ selectedPlant.name }}</h3>
                  <span class="popover-subtitle">{{ selectedPlantMeta.typeName }}</span>
                </div>
              </div>
              <div class="popover-level" :style="{ color: selectedPlantMeta.color, background: selectedPlantMeta.color + '15', borderColor: selectedPlantMeta.color + '40' }">
                {{ selectedPlantMeta.levelName }}
              </div>
              <div class="popover-rows">
                <div class="popover-row">
                  <span class="popover-row-label">装机容量</span>
                  <span class="popover-row-value">{{ selectedPlant.capacity }} MW</span>
                </div>
                <div class="popover-row">
                  <span class="popover-row-label">运行状态</span>
                  <span class="popover-row-value">{{ selectedPlantMeta.statusName }}</span>
                </div>
                <div class="popover-row">
                  <span class="popover-row-label">所在城市</span>
                  <span class="popover-row-value">{{ selectedPlantMeta.cityName }}</span>
                </div>
                <div class="popover-row popover-row--address">
                  <span class="popover-row-label">详细地址</span>
                  <span class="popover-row-value">{{ selectedPlant.address || '暂无' }}</span>
                </div>
              </div>
              <button class="popover-detail-btn" @click="goToPlantDetail(selectedPlant)">
                查看详情 →
              </button>
              <!-- 弹窗指示箭头 -->
              <span class="popover-arrow" />
            </div>
          </Transition>

          <!-- 图注 -->
          <div class="map-caption">广东省电厂分布 · 自定义地图底图</div>
        </div>
      </div>
    </div>

    <!-- 筛选按钮 -->
    <button 
      class="filter-toggle-btn"
      :class="{ active: showFilter }"
      @click="toggleFilter"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
      </svg>
      <span>筛选</span>
      <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
    </button>

    <!-- 水文站图层切换 -->
    <button
      class="filter-toggle-btn hydro-toggle-btn"
      :class="{ active: isHydroLayerVisible }"
      @click="toggleHydroLayer"
      :style="{ left: 'calc(180px + 96px)' }"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 2 C7 9 5 13 5 16 a7 7 0 0 0 14 0 c0-3-2-7-7-14z"/>
      </svg>
      <span>水文站</span>
      <span v-if="isHydroLayerVisible && hydroStats.alert > 0" class="filter-badge">{{ hydroStats.alert }}</span>
    </button>

    <!-- 筛选面板 -->
    <Transition name="slide-fade">
      <div v-if="showFilter" class="filter-wrapper">
        <FilterBar
          :plants="plants"
          @filter-change="handleFilterChange"
          @close="showFilter = false"
        />
      </div>
    </Transition>

    <!-- 聚合模式提示（仅高德地图模式显示） -->
    <Transition name="fade">
      <div v-if="isClusterMode && isLoaded && !isPlaceholder" class="cluster-hint">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <span>缩放至 {{ clusterZoomThreshold }}+ 级查看详细信息</span>
      </div>
    </Transition>

    <!-- 图例 -->
    <div class="legend">
      <div class="legend-section">
        <div class="legend-title">预警等级</div>
        <div 
          v-for="level in warningLevels" 
          :key="level.value"
          class="legend-item"
        >
          <div 
            class="legend-dot" 
            :style="{ backgroundColor: level.color }"
          />
          <span>{{ level.label }}</span>
        </div>
      </div>
      <div class="legend-section">
        <div class="legend-title">电厂类型</div>
        <div 
          v-for="type in plantTypes" 
          :key="type.value"
          class="legend-item"
        >
          <span class="legend-icon">{{ type.icon }}</span>
          <span>{{ type.label }}</span>
        </div>
      </div>
      <div class="legend-section" v-if="isHydroLayerVisible">
        <div class="legend-title">水文站 · 水位告警</div>
        <div class="legend-item">
          <span class="legend-icon legend-drop" style="color:#22d3ee">🏠</span>
          <span>正常</span>
        </div>
        <div class="legend-item">
          <span class="legend-icon legend-drop" style="color:#00d4ff">🏠</span>
          <span>关注</span>
        </div>
        <div class="legend-item">
          <span class="legend-icon legend-drop" style="color:#fbbf24">🏠</span>
          <span>超警戒</span>
        </div>
        <div class="legend-item">
          <span class="legend-icon legend-drop" style="color:#ff2e9f">🏠</span>
          <span>超保证</span>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-value">{{ filteredPlants.length }}</span>
        <span class="stat-label">显示电厂</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-value danger">{{ dangerCount }}</span>
        <span class="stat-label">危险</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-value warning">{{ warningCount }}</span>
        <span class="stat-label">警告</span>
      </div>
      <template v-if="isHydroLayerVisible">
        <div class="stat-divider" />
        <div class="stat-item">
          <span class="stat-value hydro-stat">{{ hydroStats.total }}</span>
          <span class="stat-label">水文站</span>
        </div>
        <div class="stat-divider" />
        <div class="stat-item">
          <span class="stat-value danger">{{ hydroStats.alert }}</span>
          <span class="stat-label">超警</span>
        </div>
      </template>
    </div>

    <!-- 缩放控制提示 -->
    <div class="zoom-hint">
      <span>缩放: {{ Math.round(mapScale * 100) }}%</span>
      <button
        v-if="isPlaceholder && mapScale > 1"
        class="zoom-reset-btn"
        @click="resetMapZoom"
        title="重置缩放"
      >重置</button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner" />
      <span>地图加载中...</span>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-toast">
      <span>⚠️ {{ error }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useAmap, WARNING_COLORS, type AddMarkerOptions } from '@/composables/useAmap'
import FilterBar from './FilterBar.vue'
import type { PowerPlant, WarningLevel, HydroStation, AlertLevel } from '@/types'
import { mockDataService } from '@/mock/data'

const props = withDefaults(defineProps<{
  plants: PowerPlant[]
  /** 水文站列表（可选，默认从 mock 数据获取） */
  hydroStations?: HydroStation[]
  /** 是否显示水文站图层 */
  showHydroLayer?: boolean
}>(), {
  hydroStations: undefined,
  showHydroLayer: true
})

const emit = defineEmits<{
  (e: 'plant-click', plant: PowerPlant): void
  (e: 'marker-click', plant: PowerPlant): void
  (e: 'filter-change', filters: { warningLevel: string; type: string }): void
}>()

// ======================== 状态定义 ========================
const mapContainerRef = ref<HTMLElement | null>(null)
/** 占位地图容器（用于 ResizeObserver 计算图片实际显示尺寸） */
const placeholderMapRef = ref<HTMLElement | null>(null)
/** 地图舞台元素（保持图片宽高比） */
const mapStageRef = ref<HTMLElement | null>(null)

// 高德地图实例
const {
  isLoaded,
  isPlaceholder,
  error,
  initMap,
  addMarkers,
  clearMarkers,
  onMarkerClick,
  onMapClick,
  onZoomChange,
  panTo,
  setFitView
} = useAmap()

// 地图实例引用
const amapInstance = ref<any>(null)

// 信息窗口实例
const infoWindowInstance = ref<any>(null)

// 筛选相关状态
const showFilter = ref(false)
const filterWarningLevel = ref<WarningLevel | 'all'>('all')
const filterType = ref<string>('all')

// ======================== 水文站图层状态 ========================
/** 水文站列表（优先用 prop，否则从 mock 取） */
const hydroStationList = computed<HydroStation[]>(
  () => props.hydroStations ?? mockDataService.getHydroStations()
)
/** 是否显示水文站图层 */
const isHydroLayerVisible = ref(props.showHydroLayer)

/** 告警等级 -> 颜色（赛博朋克霓虹配色） */
const HYDRO_ALERT_COLORS: Record<AlertLevel, string> = {
  normal: '#22d3ee',  // 青色 - 正常
  watch: '#00d4ff',   // 电光蓝 - 关注
  warning: '#fbbf24', // 琥珀黄 - 超警
  flood: '#ff2e9f'    // 品红 - 超保
}
/** 告警等级 -> 名称 */
const HYDRO_ALERT_NAMES: Record<AlertLevel, string> = {
  normal: '正常',
  watch: '关注',
  warning: '超警',
  flood: '超保'
}
/** 水势图标 */
const TREND_ICON: Record<string, string> = {
  rising: '▲',
  falling: '▼',
  steady: '—'
}
const TREND_NAME: Record<string, string> = {
  rising: '涨',
  falling: '落',
  steady: '平'
}

/** 渲染占位图用的水文站点集 */
const hydroPoints = computed(() => {
  if (!isHydroLayerVisible.value) return []
  return hydroStationList.value.map(st => {
    const { xPct, yPct } = projectToMapPct(st.location.lng, st.location.lat)
    const reading = st.current
    const alertLevel = reading?.alertLevel ?? 'normal'
    const color = HYDRO_ALERT_COLORS[alertLevel]
    const isAlert = alertLevel === 'warning' || alertLevel === 'flood'
    return { station: st, xPct, yPct, reading, alertLevel, color, isAlert }
  })
})

/** 选中的水文站（弹窗） */
const selectedHydroStation = ref<HydroStation | null>(null)

const selectedHydroStationPos = computed(() => {
  if (!selectedHydroStation.value) return { xPct: 0, yPct: 0 }
  return projectToMapPct(
    selectedHydroStation.value.location.lng,
    selectedHydroStation.value.location.lat
  )
})

/** 水文站弹窗方向自适应 */
const selectedHydroFlipDown = computed(() => {
  if (!selectedHydroStation.value) return false
  return selectedHydroStationPos.value.yPct < 45
})

/** 水文站弹窗水平定位自适应 */
const selectedHydroHShift = computed(() => {
  if (!selectedHydroStation.value) return 'center'
  const x = selectedHydroStationPos.value.xPct
  if (x < 18) return 'left'
  if (x > 82) return 'right'
  return 'center'
})

function handleHydroPinClick(station: HydroStation) {
  selectedHydroStation.value = station
  selectedPlant.value = null
}

function toggleHydroLayer() {
  isHydroLayerVisible.value = !isHydroLayerVisible.value
  if (!isHydroLayerVisible.value) {
    selectedHydroStation.value = null
  }
}

/** 水文站统计 */
const hydroStats = computed(() => {
  const list = hydroStationList.value
  const alertCount = list.filter(s => {
    const lv = s.current?.alertLevel
    return lv === 'warning' || lv === 'flood'
  }).length
  const floodCount = list.filter(s => s.current?.alertLevel === 'flood').length
  return { total: list.length, alert: alertCount, flood: floodCount }
})

// 聚合相关状态
const clusterZoomThreshold = 10
const currentZoom = ref(8)
const isClusterMode = computed(() => currentZoom.value < clusterZoomThreshold)

// ======================== 占位图缩放（鼠标滚轮）========================
/** 缩放比例（1 = 适应容器的基础尺寸） */
const mapScale = ref(1)
const MIN_SCALE = 1
const MAX_SCALE = 4
/** 缩放原点（百分比，相对地图舞台）*/
const mapTransformOrigin = ref({ x: 50, y: 50 })

/** 地图舞台内联样式：应用缩放与变换原点 */
const mapStageStyle = computed(() => ({
  transform: `scale(${mapScale.value})`,
  transformOrigin: `${mapTransformOrigin.value.x}% ${mapTransformOrigin.value.y}%`,
  transition: 'transform 0.12s ease-out'
}))

/** 滚轮缩放处理 */
function handleWheelZoom(e: WheelEvent) {
  if (!isPlaceholder.value) return
  // 计算鼠标相对舞台的百分比位置，作为缩放原点
  const stage = mapStageRef.value
  const container = placeholderMapRef.value
  if (stage && container) {
    const sRect = stage.getBoundingClientRect()
    if (sRect.width > 0 && sRect.height > 0) {
      const xPct = ((e.clientX - sRect.left) / sRect.width) * 100
      const yPct = ((e.clientY - sRect.top) / sRect.height) * 100
      mapTransformOrigin.value = {
        x: Math.max(0, Math.min(100, xPct)),
        y: Math.max(0, Math.min(100, yPct))
      }
    }
  }
  const delta = e.deltaY > 0 ? -0.15 : 0.15
  const next = Math.max(MIN_SCALE, Math.min(MAX_SCALE, mapScale.value + delta * mapScale.value))
  mapScale.value = Number(next.toFixed(3))
  // 同步 currentZoom（用于 UI 提示）
  currentZoom.value = Math.round(8 + (mapScale.value - 1) * 4)
}

/** 重置缩放 */
function resetMapZoom() {
  mapScale.value = 1
  mapTransformOrigin.value = { x: 50, y: 50 }
  currentZoom.value = 8
}

// 聚合点存储
interface ClusterPoint {
  position: [number, number]
  count: number
  plants: PowerPlant[]
  dominantLevel: WarningLevel
  dominantType: string
}
const clusterMarkers = ref<any[]>([])

// ======================== 常量配置 ========================
const warningLevels = [
  { value: 'green', label: '正常', color: WARNING_COLORS.green },
  { value: 'blue', label: '关注', color: WARNING_COLORS.blue },
  { value: 'yellow', label: '警告', color: WARNING_COLORS.yellow },
  { value: 'orange', label: '严重', color: WARNING_COLORS.orange },
  { value: 'red', label: '紧急', color: WARNING_COLORS.red }
]

const plantTypes = [
  { value: 'coal', label: '火电', icon: '🏭' },
  { value: 'gas', label: '燃气', icon: '⚡' },
  { value: 'solar', label: '光伏', icon: '☀️' },
  { value: 'wind', label: '风电', icon: '🌬️' },
  { value: 'storage', label: '储能', icon: '🔋' }
]

const plantTypeNames: Record<string, string> = {
  coal: '火电厂',
  gas: '燃气电厂',
  solar: '光伏电站',
  wind: '风电场',
  storage: '储能站'
}

const warningLevelNames: Record<WarningLevel, string> = {
  green: '正常',
  blue: '关注',
  yellow: '警告',
  orange: '严重',
  red: '紧急'
}

// ======================== 占位地图（自定义图片底图）====================
// 经纬度 -> 图片百分比，自动适配容器尺寸
/** 经纬度范围（覆盖广东省陆地 + 周边海域） */
const MAP_BOUNDS = {
  minLng: 109.55,
  maxLng: 117.40,
  minLat: 20.00,
  maxLat: 25.55
}

/** 地图图片原始尺寸（宽高比用于校准投影，新水系图 3690×2599） */
const MAP_IMAGE_RATIO = 3690 / 2599 // ≈ 1.420

/**
 * 经纬度 → 地图图片百分比坐标。
 * 关键：图片用 object-fit: contain 渲染，实际图片区域可能小于容器。
 * 这里把经纬度先映射到「图片内容」的 0~100%，再由 CSS 容器自适应处理。
 * 地图朝向上北上西（左上原点），经度越大越靠右，纬度越大越靠上。
 */
function projectToMapPct(lng: number, lat: number): { xPct: number; yPct: number } {
  const { minLng, maxLng, minLat, maxLat } = MAP_BOUNDS
  // 广东陆地大致占图片 [4%, 96%] x [6%, 92%]（预留标题/图例区）
  const xPct = 4 + ((lng - minLng) / (maxLng - minLng)) * 92
  const yPct = 6 + ((maxLat - lat) / (maxLat - minLat)) * 86
  return { xPct: Math.max(1, Math.min(99, xPct)), yPct: Math.max(1, Math.min(99, yPct)) }
}

/** 电厂类型 emoji（点位上显示） */
const plantTypeEmoji: Record<string, string> = {
  coal: '🏭',
  gas: '⚡',
  solar: '☀️',
  wind: '🌬️',
  storage: '🔋'
}

/** 电厂类型缩写（点位字符） */
const plantTypeIcon: Record<string, string> = {
  coal: 'F',
  gas: 'G',
  solar: 'S',
  wind: 'W',
  storage: 'B'
}

/** 主要城市参考点（按经纬度映射到图片百分比） */
const referenceCities = [
  { name: '广州', lng: 113.27, lat: 23.13 },
  { name: '深圳', lng: 114.06, lat: 22.55 },
  { name: '珠海', lng: 113.58, lat: 22.28 },
  { name: '汕头', lng: 116.68, lat: 23.35 },
  { name: '湛江', lng: 110.36, lat: 21.27 },
  { name: '韶关', lng: 113.60, lat: 24.81 }
].map(c => ({ name: c.name, ...projectToMapPct(c.lng, c.lat) }))

/** 渲染占位图用的电厂点集 */
const placeholderPoints = computed(() => {
  return filteredPlants.value.map(plant => {
    const { xPct, yPct } = projectToMapPct(plant.location.lng, plant.location.lat)
    const wl = plant.warningLevel || 'green'
    const isWarning = wl === 'yellow' || wl === 'orange' || wl === 'red'
    const color = WARNING_COLORS[wl] || WARNING_COLORS.green
    const emoji = plantTypeEmoji[plant.type] || '🏭'
    return { plant, xPct, yPct, isWarning, color, emoji }
  })
})

/** 占位图上点击电厂点：仅显示信息弹窗，跳转由弹窗内"查看详情"按钮触发 */
function handlePlaceholderClick(plant: PowerPlant) {
  selectedPlant.value = plant
  selectedHydroStation.value = null
}

// ======================== 自定义 HTML 信息弹窗 ========================
const selectedPlant = ref<PowerPlant | null>(null)

const selectedPlantPos = computed(() => {
  if (!selectedPlant.value) return { xPct: 0, yPct: 0 }
  return projectToMapPct(selectedPlant.value.location.lng, selectedPlant.value.location.lat)
})

/**
 * 弹窗方向自适应：点位位于舞台上半部时弹窗向下显示，位于下半部时向上显示。
 * 避免弹窗溢出 .placeholder-map 的 overflow:hidden 区域被裁剪。
 */
const selectedPlantFlipDown = computed(() => {
  if (!selectedPlant.value) return false
  return selectedPlantPos.value.yPct < 45
})

/**
 * 水平定位自适应：点位靠近左/右边缘时，弹窗对齐到边缘避免横向溢出被裁剪。
 * - xPct < 18：贴左对齐（shift-left）
 * - xPct > 82：贴右对齐（shift-right）
 * - 中间区域：保持居中
 */
const selectedPlantHShift = computed(() => {
  if (!selectedPlant.value) return 'center'
  const x = selectedPlantPos.value.xPct
  if (x < 18) return 'left'
  if (x > 82) return 'right'
  return 'center'
})

const selectedPlantMeta = computed(() => {
  const p = selectedPlant.value
  if (!p) {
    return { icon: '', typeName: '', levelName: '', color: '#10b981', statusName: '', cityName: '' }
  }
  const typeName = plantTypeNames[p.type] || p.type
  const levelName = warningLevelNames[p.warningLevel || 'green']
  const color = WARNING_COLORS[p.warningLevel || 'green'] || WARNING_COLORS.green
  const icon = plantTypeEmoji[p.type] || '🏭'
  const statusMap: Record<string, string> = {
    normal: '正常运行',
    warning: '预警中',
    danger: '紧急告警',
    offline: '离线'
  }
  const cityMatch = p.address?.match(/^广东省(.+?)市/)
  const cityName = cityMatch?.[1] || '—'
  return {
    icon,
    typeName,
    levelName,
    color,
    statusName: statusMap[p.status] || p.status,
    cityName
  }
})

function goToPlantDetail(plant: PowerPlant) {
  emit('plant-click', plant)
  selectedPlant.value = null
}

// 关闭弹窗：点击地图空白区域
function closePopover() {
  selectedPlant.value = null
  selectedHydroStation.value = null
}

// ======================== 计算属性 ========================
const loading = computed(() => !isLoaded.value)

// 激活的筛选数量
const activeFilterCount = computed(() => {
  let count = 0
  if (filterWarningLevel.value !== 'all') count++
  if (filterType.value !== 'all') count++
  return count
})

// 筛选后的电厂列表
const filteredPlants = computed(() => {
  let result = [...props.plants]
  
  if (filterWarningLevel.value !== 'all') {
    result = result.filter(p => p.warningLevel === filterWarningLevel.value)
  }
  
  if (filterType.value !== 'all') {
    result = result.filter(p => p.type === filterType.value)
  }
  
  return result
})

// 危险数量
const dangerCount = computed(() => 
  filteredPlants.value.filter(p => p.warningLevel === 'red').length
)

// 警告数量
const warningCount = computed(() => 
  filteredPlants.value.filter(p => p.warningLevel === 'orange' || p.warningLevel === 'yellow').length
)

// ======================== 方法定义 ========================

// 切换筛选面板
function toggleFilter() {
  showFilter.value = !showFilter.value
}

// 处理筛选变更
function handleFilterChange(filters: { warningLevel: WarningLevel | 'all'; type: string }) {
  filterWarningLevel.value = filters.warningLevel
  filterType.value = filters.type
  
  emit('filter-change', filters)
  
  // 重新渲染标记
  renderMarkers()
}

// 创建单个电厂标记内容
function createMarkerContent(plant: PowerPlant): string {
  const color = WARNING_COLORS[plant.warningLevel || 'green']
  const icons: Record<string, string> = {
    coal: '🏭', gas: '⚡', solar: '☀️', wind: '🌬️', storage: '🔋'
  }
  const icon = icons[plant.type] || '🏭'
  const isWarning = plant.warningLevel && ['yellow', 'orange', 'red'].includes(plant.warningLevel)

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
      font-size: 16px;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(0,0,0,0.3);
      ${isWarning ? 'animation: warningPulse 1.5s ease-in-out infinite;' : ''}
    ">
      ${icon}
    </div>
    <style>
      @keyframes warningPulse {
        0%, 100% { transform: scale(1); }
        50% { transform: scale(1.1); }
      }
    </style>
  `
}

// 创建聚合标记内容
function createClusterContent(cluster: ClusterPoint): string {
  const color = WARNING_COLORS[cluster.dominantLevel]
  
  // 统计各类型数量
  const typeCounts: Record<string, number> = {}
  cluster.plants.forEach(p => {
    typeCounts[p.type] = (typeCounts[p.type] || 0) + 1
  })
  
  // 找出最多数量的类型
  const dominantType = Object.entries(typeCounts).sort((a, b) => b[1] - a[1])[0]
  const typeIcon: Record<string, string> = {
    coal: '🏭', gas: '⚡', solar: '☀️', wind: '🌬️', storage: '🔋'
  }
  
  // 判断是否有危险等级
  const hasDanger = cluster.plants.some(p => p.warningLevel === 'red')
  const hasWarning = cluster.plants.some(p => ['orange', 'yellow'].includes(p.warningLevel || ''))
  
  const size = Math.min(60, 36 + cluster.count * 2)
  
  return `
    <div style="
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: ${size}px;
      height: ${size}px;
      background: linear-gradient(135deg, ${color} 0%, ${hasDanger ? '#dc2626' : hasWarning ? '#f97316' : color} 100%);
      border: 4px solid white;
      border-radius: 50%;
      box-shadow: 0 4px 16px rgba(0,0,0,0.3);
      cursor: pointer;
      ${hasDanger || hasWarning ? 'animation: clusterPulse 2s ease-in-out infinite;' : ''}
    ">
      <span style="font-size: ${size > 45 ? 18 : 14}px;">${typeIcon[dominantType[0]] || '🏭'}</span>
      <span style="font-size: 12px; font-weight: 700; color: white; text-shadow: 0 1px 2px rgba(0,0,0,0.3);">${cluster.count}</span>
    </div>
    <style>
      @keyframes clusterPulse {
        0%, 100% { transform: scale(1); box-shadow: 0 4px 16px rgba(0,0,0,0.3); }
        50% { transform: scale(1.05); box-shadow: 0 6px 24px rgba(0,0,0,0.4); }
      }
    </style>
  `
}

// 创建信息窗口内容
function createInfoWindowContent(plant: PowerPlant): string {
  const color = WARNING_COLORS[plant.warningLevel || 'green']
  const typeName = plantTypeNames[plant.type] || plant.type
  const levelName = warningLevelNames[plant.warningLevel || 'green']
  
  return `
    <div style="padding: 16px; min-width: 260px; font-family: -apple-system, sans-serif;">
      <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 12px; border-bottom: 1px solid #e5e7eb; padding-bottom: 12px;">
        <span style="font-size: 24px;">${plantTypes.find(t => t.value === plant.type)?.icon || '🏭'}</span>
        <div>
          <h3 style="margin: 0; font-size: 16px; font-weight: 600; color: #1f2937;">${plant.name}</h3>
          <span style="font-size: 12px; color: #6b7280;">${typeName}</span>
        </div>
      </div>
      
      <div style="margin-bottom: 12px;">
        <span style="display: inline-block; padding: 4px 10px; background: ${color}15; color: ${color}; font-size: 12px; font-weight: 600; border-radius: 12px; border: 1px solid ${color}40;">
          ${levelName}
        </span>
      </div>
      
      <div style="font-size: 13px; color: #4b5563; line-height: 1.8;">
        <div style="display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px dashed #f3f4f6;">
          <span style="color: #9ca3af;">装机容量</span>
          <span style="font-weight: 500;">${plant.capacity} MW</span>
        </div>
        <div style="display: flex; justify-content: space-between; padding: 4px 0;">
          <span style="color: #9ca3af;">详细地址</span>
          <span style="font-weight: 500;">${plant.address || '暂无'}</span>
        </div>
      </div>
      
      <div style="margin-top: 14px; padding-top: 12px; border-top: 1px solid #e5e7eb;">
        <button 
          id="view-detail-btn-${plant.id}"
          style="width: 100%; padding: 10px; background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%); color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 500;"
        >
          查看详情 →
        </button>
      </div>
    </div>
  `
}

// 创建聚合信息窗口内容
function createClusterInfoWindowContent(cluster: ClusterPoint): string {
  const levelCounts: Record<string, number> = {}
  const typeCounts: Record<string, number> = {}
  
  cluster.plants.forEach(p => {
    levelCounts[p.warningLevel || 'green'] = (levelCounts[p.warningLevel || 'green'] || 0) + 1
    typeCounts[p.type] = (typeCounts[p.type] || 0) + 1
  })
  
  const levelItems = Object.entries(levelCounts)
    .sort((a, b) => {
      const order = ['red', 'orange', 'yellow', 'blue', 'green']
      return order.indexOf(a[0]) - order.indexOf(b[0])
    })
    .map(([level, count]) => {
      const color = WARNING_COLORS[level as WarningLevel]
      const name = warningLevelNames[level as WarningLevel]
      return `<span style="display: inline-flex; align-items: center; gap: 4px; padding: 3px 8px; background: ${color}15; color: ${color}; border-radius: 4px; font-size: 12px; margin: 2px;">${name} ${count}</span>`
    }).join('')
  
  const typeItems = Object.entries(typeCounts)
    .map(([type, count]) => {
      const icon = { coal: '🏭', gas: '⚡', solar: '☀️', wind: '🌬️', storage: '🔋' }[type] || '🏭'
      const name = plantTypeNames[type] || type
      return `<span style="display: inline-flex; align-items: center; gap: 4px; padding: 3px 8px; background: #f3f4f6; border-radius: 4px; font-size: 12px; margin: 2px;">${icon} ${name} ${count}</span>`
    }).join('')
  
  return `
    <div style="padding: 16px; min-width: 280px; font-family: -apple-system, sans-serif;">
      <h3 style="margin: 0 0 12px 0; font-size: 16px; font-weight: 600; color: #1f2937; display: flex; align-items: center; gap: 8px;">
        <span>📍</span>
        <span>区域聚合</span>
        <span style="background: #0ea5e9; color: white; padding: 2px 8px; border-radius: 10px; font-size: 12px;">${cluster.count} 个电厂</span>
      </h3>
      
      <div style="margin-bottom: 12px;">
        <div style="font-size: 12px; color: #6b7280; margin-bottom: 6px;">预警等级分布</div>
        <div>${levelItems}</div>
      </div>
      
      <div style="margin-bottom: 12px;">
        <div style="font-size: 12px; color: #6b7280; margin-bottom: 6px;">电厂类型分布</div>
        <div>${typeItems}</div>
      </div>
      
      <div style="padding-top: 12px; border-top: 1px solid #e5e7eb;">
        <button 
          id="cluster-detail-btn"
          style="width: 100%; padding: 10px; background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%); color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 500;"
        >
          查看该区域详情 →
        </button>
      </div>
    </div>
  `
}

// 简单网格聚合算法
function clusterPlants(plants: PowerPlant[], gridSize: number = 0.5): ClusterPoint[] {
  if (plants.length === 0) return []
  
  const clusters: Map<string, ClusterPoint> = new Map()
  
  plants.forEach(plant => {
    // 根据网格大小分组
    const gridX = Math.floor(plant.location.lng / gridSize)
    const gridY = Math.floor(plant.location.lat / gridSize)
    const key = `${gridX},${gridY}`
    
    if (!clusters.has(key)) {
      clusters.set(key, {
        position: [0, 0],
        count: 0,
        plants: [],
        dominantLevel: 'green',
        dominantType: 'coal'
      })
    }
    
    const cluster = clusters.get(key)!
    cluster.plants.push(plant)
    cluster.count++
    
    // 更新中心位置（加权平均）
    const weight = cluster.count
    cluster.position = [
      (cluster.position[0] * (weight - 1) + plant.location.lng) / weight,
      (cluster.position[1] * (weight - 1) + plant.location.lat) / weight
    ]
    
    // 更新主等级（取最严重的）
    const levelPriority: Record<string, number> = { red: 5, orange: 4, yellow: 3, blue: 2, green: 1 }
    if (levelPriority[plant.warningLevel || 'green'] > levelPriority[cluster.dominantLevel]) {
      cluster.dominantLevel = plant.warningLevel || 'green'
    }
    
    // 更新主类型（取最常见的）
    const typeCounts: Record<string, number> = {}
    cluster.plants.forEach(p => {
      typeCounts[p.type] = (typeCounts[p.type] || 0) + 1
    })
    cluster.dominantType = Object.entries(typeCounts).sort((a, b) => b[1] - a[1])[0][0]
  })
  
  return Array.from(clusters.values())
}

// 渲染标记点
function renderMarkers() {
  if (!amapInstance.value) return
  
  // 清空现有标记
  clearAllMarkers()
  
  if (filteredPlants.value.length === 0) return
  
  if (isClusterMode.value) {
    // 聚合模式
    renderClusterMarkers()
  } else {
    // 单个标记模式
    renderSingleMarkers()
  }
}

// 渲染单个标记
function renderSingleMarkers() {
  const markerOptions: AddMarkerOptions[] = filteredPlants.value.map(plant => ({
    id: plant.id,
    position: [plant.location.lng, plant.location.lat],
    title: plant.name,
    content: createMarkerContent(plant),
    extData: plant,
    clickable: true,
    animation: 'AMAP_ANIMATION_DROP',
    zIndex: plant.warningLevel === 'red' ? 1000 : plant.warningLevel === 'orange' ? 900 : 100
  }))
  
  addMarkers(markerOptions)
}

// 渲染聚合标记
function renderClusterMarkers() {
  if (!amapInstance.value || !window.AMap) return
  
  const clusters = clusterPlants(filteredPlants.value)
  
  clusters.forEach((cluster, index) => {
    const marker = new window.AMap.Marker({
      position: cluster.position,
      content: createClusterContent(cluster),
      extData: cluster,
      clickable: true,
      zIndex: cluster.dominantLevel === 'red' ? 1000 : 100,
      offset: new window.AMap.Pixel(-30, -30)
    })
    
    marker.setMap(amapInstance.value)
    clusterMarkers.value.push(marker)
    
    // 点击聚合点
    marker.on('click', () => {
      handleClusterClick(cluster)
    })
  })
}

// 处理聚合点点击
function handleClusterClick(cluster: ClusterPoint) {
  // 放大地图到该区域
  panTo(cluster.position)
  if (amapInstance.value) {
    amapInstance.value.setZoom(clusterZoomThreshold)
  }
}

// 清空所有标记
function clearAllMarkers() {
  clearMarkers()
  
  // 清空聚合标记
  clusterMarkers.value.forEach(marker => {
    marker.setMap(null)
  })
  clusterMarkers.value = []
}

// 打开信息窗口
function openInfoWindow(plant: PowerPlant) {
  if (!window.AMap || !amapInstance.value) return

  closeInfoWindow()

  const infoContent = createInfoWindowContent(plant)
  
  infoWindowInstance.value = new window.AMap.InfoWindow({
    content: infoContent,
    closeWhenClickMap: true,
    offset: new window.AMap.Pixel(0, -30),
    position: [plant.location.lng, plant.location.lat]
  })
  
  infoWindowInstance.value.open(amapInstance.value)
  
  // 延迟绑定详情按钮事件
  setTimeout(() => {
    const btn = document.getElementById(`view-detail-btn-${plant.id}`)
    if (btn) {
      btn.addEventListener('click', () => {
        emit('plant-click', plant)
        closeInfoWindow()
      })
    }
  }, 100)
}

// 打开聚合信息窗口
function openClusterInfoWindow(cluster: ClusterPoint) {
  if (!window.AMap || !amapInstance.value) return

  closeInfoWindow()

  const infoContent = createClusterInfoWindowContent(cluster)
  
  infoWindowInstance.value = new window.AMap.InfoWindow({
    content: infoContent,
    closeWhenClickMap: true,
    offset: new window.AMap.Pixel(0, -40),
    position: cluster.position
  })
  
  infoWindowInstance.value.open(amapInstance.value)
}

// 关闭信息窗口
function closeInfoWindow() {
  if (infoWindowInstance.value) {
    infoWindowInstance.value.close()
    infoWindowInstance.value = null
  }
}

// 处理标记点击
function handleMarkerClick(marker: any) {
  const plant = marker.data as PowerPlant
  emit('marker-click', plant)
  openInfoWindow(plant)
}

// 初始化地图
async function initAmap() {
  if (!mapContainerRef.value) return

  const mapId = 'power-plant-map-container'
  
  amapInstance.value = await initMap(mapId, {
    zoom: 8,
    center: [113.280637, 23.125178],
    viewMode: '2D',
    mapStyle: 'amap://styles/normal'
  })

  if (amapInstance.value) {
    // 渲染初始标记
    renderMarkers()
    
    // 监听标记点击
    onMarkerClick(handleMarkerClick)
    
    // 监听地图点击（关闭信息窗口）
    onMapClick(() => {
      closeInfoWindow()
    })
    
    // 监听缩放变化
    onZoomChange((zoom) => {
      currentZoom.value = zoom
      renderMarkers()
    })
  }
}

// 跳转到指定电厂
function goToPlant(plantId: string) {
  const plant = filteredPlants.value.find(p => p.id === plantId)
  if (plant) {
    panTo([plant.location.lng, plant.location.lat])
    setTimeout(() => {
      if (amapInstance.value) {
        amapInstance.value.setZoom(clusterZoomThreshold)
      }
    }, 100)
    const marker = { data: plant }
    handleMarkerClick(marker)
  }
}

// 聚焦所有电厂
function fitAll() {
  setFitView(50)
}

// 暴露方法
defineExpose({
  goToPlant,
  fitAll,
  renderMarkers,
  toggleFilter,
  getFilters: () => ({
    warningLevel: filterWarningLevel.value,
    type: filterType.value
  })
})

// ======================== 生命周期 ========================
/** ResizeObserver：保持 .map-stage 与地图图片宽高比一致 */
let stageResizeObserver: ResizeObserver | null = null

function updateStageSize() {
  const container = placeholderMapRef.value
  const stage = mapStageRef.value
  if (!container || !stage) return
  const cw = container.clientWidth
  const ch = container.clientHeight
  if (cw === 0 || ch === 0) return
  // 图片宽高比 1024/837 ≈ 1.223（宽 > 高）
  // 按容器较小维度铺满并保持比例
  const ratio = MAP_IMAGE_RATIO
  let w: number, h: number
  if (cw / ch > ratio) {
    // 容器更宽：按高度铺满
    h = ch
    w = h * ratio
  } else {
    // 容器更高：按宽度铺满
    w = cw
    h = w / ratio
  }
  stage.style.width = w + 'px'
  stage.style.height = h + 'px'
}

watch(
  () => [placeholderMapRef.value, mapStageRef.value, isPlaceholder.value],
  () => {
    if (isPlaceholder.value && placeholderMapRef.value && !stageResizeObserver) {
      stageResizeObserver = new ResizeObserver(updateStageSize)
      stageResizeObserver.observe(placeholderMapRef.value)
      updateStageSize()
    }
  },
  { flush: 'post' }
)

watch(isPlaceholder, (ph) => {
  if (ph) {
    nextTick(updateStageSize)
  }
})

watch(
  () => props.plants,
  () => {
    if (isLoaded.value) {
      renderMarkers()
    }
  },
  { deep: true }
)

watch(isLoaded, (loaded) => {
  if (loaded) {
    renderMarkers()
  }
})

onMounted(() => {
  initAmap()
  // 占位模式下，初始化舞台尺寸
  if (isPlaceholder.value) {
    nextTick(updateStageSize)
  }
})

onUnmounted(() => {
  clearAllMarkers()
  if (stageResizeObserver) {
    stageResizeObserver.disconnect()
    stageResizeObserver = null
  }
})
</script>

<style scoped>
.power-plant-map {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 500px;
  /* 大屏 HUD 主题变量 */
  --hud-bg1: rgba(10, 22, 40, 0.92);
  --hud-bg2: rgba(4, 10, 24, 0.95);
  --hud-cyan: #00d4ff;
  --hud-magenta: #ff2e9f;
  --hud-amber: #fbbf24;
  --hud-text: #cbd5e1;
  --hud-text-dim: #94a3b8;
  --hud-border: rgba(0, 212, 255, 0.4);
  --hud-glow: rgba(0, 212, 255, 0.18);
}

/* 大屏 HUD 面板共用网格底纹 */
.legend,
.stats-bar,
.plant-popover,
.hydro-popover,
.cluster-hint {
  background-image:
    linear-gradient(135deg, var(--hud-bg1) 0%, var(--hud-bg2) 100%),
    repeating-linear-gradient(0deg, transparent 0 18px, rgba(0, 212, 255, 0.045) 18px 19px),
    repeating-linear-gradient(90deg, transparent 0 18px, rgba(0, 212, 255, 0.045) 18px 19px);
  border: 1px solid var(--hud-border);
  box-shadow:
    0 0 0 1px rgba(0, 212, 255, 0.06),
    0 4px 24px rgba(0, 0, 0, 0.6),
    0 0 22px var(--hud-glow);
}

/* 大屏 HUD 四角 L 型角标装饰 */
.legend::before,
.stats-bar::before,
.plant-popover::before,
.hydro-popover::before,
.cluster-hint::before {
  content: '';
  position: absolute;
  inset: -1px;
  pointer-events: none;
  z-index: 1;
  background:
    linear-gradient(var(--hud-cyan), var(--hud-cyan)) top left / 14px 2px no-repeat,
    linear-gradient(var(--hud-cyan), var(--hud-cyan)) top left / 2px 14px no-repeat,
    linear-gradient(var(--hud-cyan), var(--hud-cyan)) top right / 14px 2px no-repeat,
    linear-gradient(var(--hud-cyan), var(--hud-cyan)) top right / 2px 14px no-repeat,
    linear-gradient(var(--hud-cyan), var(--hud-cyan)) bottom left / 14px 2px no-repeat,
    linear-gradient(var(--hud-cyan), var(--hud-cyan)) bottom left / 2px 14px no-repeat,
    linear-gradient(var(--hud-cyan), var(--hud-cyan)) bottom right / 14px 2px no-repeat,
    linear-gradient(var(--hud-cyan), var(--hud-cyan)) bottom right / 2px 14px no-repeat;
  filter: drop-shadow(0 0 3px rgba(0, 212, 255, 0.65));
}

.map-container {
  width: 100%;
  height: 100%;
  border-radius: 2px;
  overflow: hidden;
  background: #0a0e27;
  position: relative;
  box-shadow: inset 0 0 0 1px rgba(0, 212, 255, 0.25), inset 0 0 60px rgba(0, 212, 255, 0.08);
}

/* ======================== 占位地图（自定义图片底图）==================== */
.placeholder-map {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at center, #0f1535 0%, #07091a 100%);
  z-index: 1;
  overflow: hidden;
}

/*
 * 关键修复：让 .map-stage 保持与地图图片相同的宽高比（1024:837 ≈ 1.223），
 * 这样图片完全填满 stage，百分比坐标 (left/top %) 才能准确映射到图片像素位置。
 * stage 尺寸由 JS ResizeObserver 计算（见 script），这里只做基础布局。
 */
.map-stage {
  position: relative;
  margin: auto;
  flex: 0 0 auto;
}

.map-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: fill;
  display: block;
  user-select: none;
  -webkit-user-drag: none;
  filter:
    brightness(1.1) contrast(1.05) saturate(1.1)
    drop-shadow(0 12px 28px rgba(0, 0, 0, 0.5));
}

/* 城市参考标签 */
.city-labels {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.city-label {
  position: absolute;
  transform: translate(-50%, -50%);
  font-size: 11px;
  color: rgba(0, 212, 255, 0.9);
  background: rgba(10, 14, 39, 0.65);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
  white-space: nowrap;
  font-family: -apple-system, 'PingFang SC', sans-serif;
  text-shadow: 0 0 6px rgba(0, 212, 255, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.2);
}

/* 电厂点位层（绝对定位在地图上） */
.plant-points {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.plant-pin {
  position: absolute;
  transform: translate(-50%, -50%);
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.95);
  /* 多层白色光晕，让标注圈在深色地图上更醒目 */
  box-shadow:
    0 0 4px rgba(255, 255, 255, 0.9),
    0 0 10px rgba(255, 255, 255, 0.55),
    0 0 18px rgba(255, 255, 255, 0.3),
    0 2px 6px rgba(0, 0, 0, 0.6);
  cursor: pointer;
  pointer-events: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 5px;
  font-weight: 700;
  color: #ffffff;
  transition: transform 0.18s ease-out, box-shadow 0.18s ease-out;
  z-index: 5;
  padding: 0;
  background: rgba(7, 9, 26, 0.55);
}

.plant-pin:hover {
  transform: translate(-50%, -50%) scale(1.6);
  box-shadow:
    0 0 6px rgba(255, 255, 255, 1),
    0 0 14px rgba(255, 255, 255, 0.7),
    0 0 26px rgba(255, 255, 255, 0.4),
    0 2px 8px rgba(0, 0, 0, 0.6);
  z-index: 8;
}

.plant-pin-icon {
  font-family: 'Apple Color Emoji', 'Segoe UI Emoji', 'Noto Color Emoji', -apple-system, sans-serif;
  line-height: 1;
}

.plant-pin-pulse {
  position: absolute;
  inset: -1.5px;
  border-radius: 50%;
  border: 1.5px solid var(--pin-warn-color, #00d4ff);
  opacity: 0;
  pointer-events: none;
}

.plant-pin--warning .plant-pin-pulse {
  animation: pinPulse 1.6s ease-out infinite;
}

@keyframes pinPulse {
  0% {
    transform: scale(0.9);
    opacity: 0.7;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

/* ======================== 水文站点位 ======================== */
.hydro-points {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 6;
}

.hydro-pin {
  position: absolute;
  transform: translate(-50%, -100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  cursor: pointer;
  pointer-events: auto;
  padding: 0;
  border: none;
  background: transparent;
  transition: transform 0.18s ease-out;
  z-index: 6;
}

.hydro-pin:hover {
  transform: translate(-50%, -100%) scale(1.8);
  z-index: 9;
}

.hydro-pin-drop {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.5));
}

.hydro-pin-trend {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  font-size: 3px;
  font-weight: 700;
  line-height: 1;
  text-shadow: 0 0 2px rgba(255, 255, 255, 0.9);
}

.hydro-pin-label {
  font-size: 3px;
  font-weight: 700;
  background: rgba(10, 14, 39, 0.92);
  color: #fff;
  padding: 0 2px;
  border-radius: 1px;
  border: 1px solid;
  white-space: nowrap;
  font-family: -apple-system, 'PingFang SC', sans-serif;
  line-height: 1.3;
  text-shadow: 0 0 4px currentColor;
}

.hydro-pin-pulse {
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 9px;
  height: 9px;
  border-radius: 50%;
  border: 1px solid var(--pin-color, #ef4444);
  opacity: 0;
  pointer-events: none;
}

.hydro-pin--alert .hydro-pin-pulse {
  animation: hydroPulse 1.4s ease-out infinite;
}

@keyframes hydroPulse {
  0% {
    transform: translateX(-50%) scale(0.6);
    opacity: 0.8;
  }
  100% {
    transform: translateX(-50%) scale(1.8);
    opacity: 0;
  }
}

/* 水文站信息弹窗 */
.hydro-popover {
  position: absolute;
  transform: translate(-50%, calc(-100% - 18px));
  width: 280px;
  border-radius: 2px;
  padding: 14px 14px 12px;
  z-index: 55;
  pointer-events: auto;
  font-family: -apple-system, 'PingFang SC', sans-serif;
  backdrop-filter: blur(8px);
  color: #e2e8f0;
}

.hydro-popover .popover-arrow {
  bottom: -7px;
}

.hydro-popover-icon {
  font-size: 22px;
}

.popover-level-sub {
  margin-left: 6px;
  font-size: 11px;
  font-weight: 500;
  opacity: 0.85;
}

.hydro-value-main {
  font-size: 15px;
  font-weight: 700;
  color: #00d4ff;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
}

/* 水位条形可视化 */
.hydro-bar {
  margin-top: 12px;
  position: relative;
  z-index: 2;
}

.hydro-bar-track {
  position: relative;
  height: 10px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 5px;
  overflow: visible;
}

.hydro-bar-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.3s ease-out;
}

.hydro-bar-mark {
  position: absolute;
  top: -2px;
  width: 2px;
  height: 14px;
  background: #1f2937;
  z-index: 2;
}

.hydro-bar-warn {
  background: #f59e0b;
}

.hydro-bar-guar {
  background: #ef4444;
}

.hydro-bar-legend {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 10px;
  color: rgba(148, 163, 184, 0.8);
}

/* 水文统计值颜色 */
.stat-value.hydro-stat {
  color: var(--hud-cyan);
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.6);
}

/* 水文图层切换按钮 */
.hydro-toggle-btn {
  background: linear-gradient(135deg, var(--hud-cyan) 0%, #0099cc 100%);
  color: #04101f;
  box-shadow: 0 0 14px rgba(0, 212, 255, 0.5);
}

.hydro-toggle-btn:not(.active) {
  background: linear-gradient(135deg, rgba(10, 22, 40, 0.9), rgba(4, 10, 24, 0.92));
  color: var(--hud-text-dim);
  border: 1px solid var(--hud-border);
}

.hydro-toggle-btn.active .filter-badge {
  background: var(--hud-magenta);
  color: white;
}

.legend-drop {
  font-size: 13px;
}

/* 信息弹窗 */
.plant-popover {
  position: absolute;
  transform: translate(-50%, calc(-100% - 14px));
  width: 280px;
  border-radius: 2px;
  padding: 14px 14px 12px;
  z-index: 50;
  pointer-events: auto;
  font-family: -apple-system, 'PingFang SC', sans-serif;
  backdrop-filter: blur(8px);
  color: #e2e8f0;
}

/* 弹窗翻转到点位下方（点位靠近舞台顶部时使用，避免被 overflow:hidden 裁剪） */
.plant-popover.flip-down,
.hydro-popover.flip-down {
  transform: translate(-50%, 18px);
}

.plant-popover.flip-down .popover-arrow,
.hydro-popover.flip-down .popover-arrow {
  bottom: auto;
  top: -7px;
  box-shadow: -2px -2px 4px rgba(0, 0, 0, 0.2);
}

/* 水平边缘自适应：点位靠近左边缘时弹窗左对齐，避免左侧溢出被裁剪 */
.plant-popover.shift-left,
.hydro-popover.shift-left {
  transform: translate(0, calc(-100% - 14px));
}

.plant-popover.shift-left.flip-down,
.hydro-popover.shift-left.flip-down {
  transform: translate(0, 18px);
}

.plant-popover.shift-left .popover-arrow,
.hydro-popover.shift-left .popover-arrow {
  left: 22px;
}

/* 点位靠近右边缘时弹窗右对齐 */
.plant-popover.shift-right,
.hydro-popover.shift-right {
  transform: translate(-100%, calc(-100% - 14px));
}

.plant-popover.shift-right.flip-down,
.hydro-popover.shift-right.flip-down {
  transform: translate(-100%, 18px);
}

.plant-popover.shift-right .popover-arrow,
.hydro-popover.shift-right .popover-arrow {
  left: auto;
  right: 22px;
}

.popover-arrow {
  position: absolute;
  left: 50%;
  bottom: -7px;
  width: 14px;
  height: 14px;
  background: rgba(12, 17, 43, 0.95);
  transform: translateX(-50%) rotate(45deg);
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.popover-close {
  position: absolute;
  top: 6px;
  right: 8px;
  background: transparent;
  border: none;
  font-size: 18px;
  line-height: 1;
  color: var(--hud-text-dim);
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 2px;
  z-index: 3;
}

.popover-close:hover {
  background: rgba(0, 212, 255, 0.18);
  color: var(--hud-cyan);
  text-shadow: 0 0 6px rgba(0, 212, 255, 0.6);
}

.popover-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 212, 255, 0.25);
  margin-bottom: 10px;
  position: relative;
  z-index: 2;
}

.popover-icon {
  font-size: 26px;
}

.popover-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #f1f5f9;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.4);
}

.popover-subtitle {
  font-size: 12px;
  color: var(--hud-text-dim);
}

.popover-level {
  display: inline-block;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  border-radius: 2px;
  border: 1px solid;
  margin-bottom: 10px;
  position: relative;
  z-index: 2;
  clip-path: polygon(0 0, calc(100% - 5px) 0, 100% 5px, 100% 100%, 5px 100%, 0 calc(100% - 5px));
}

.popover-rows {
  font-size: 13px;
  color: var(--hud-text);
  line-height: 1.6;
  position: relative;
  z-index: 2;
}

.popover-row {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  border-bottom: 1px dashed rgba(0, 212, 255, 0.18);
  gap: 8px;
}

.popover-row:last-child {
  border-bottom: none;
}

.popover-row--address {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.popover-row-label {
  color: var(--hud-text-dim);
  font-size: 12px;
}

.popover-row-value {
  font-weight: 500;
  color: #e2e8f0;
  text-align: right;
}

.popover-row--address .popover-row-value {
  text-align: left;
  width: 100%;
}

.popover-detail-btn {
  margin-top: 12px;
  width: 100%;
  padding: 9px;
  background: linear-gradient(135deg, var(--hud-cyan) 0%, #0099cc 100%);
  color: #04101f;
  border: none;
  border-radius: 2px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  transition: opacity 0.15s ease-out;
  box-shadow: 0 0 14px rgba(0, 212, 255, 0.5);
  position: relative;
  z-index: 2;
  clip-path: polygon(0 0, calc(100% - 6px) 0, 100% 6px, 100% 100%, 6px 100%, 0 calc(100% - 6px));
}

.popover-detail-btn:hover {
  opacity: 0.92;
}

/* 弹窗动画 */
.info-fade-enter-active {
  transition: opacity 0.18s ease-out, transform 0.18s ease-out;
}

.info-fade-leave-active {
  transition: opacity 0.15s ease-in, transform 0.15s ease-in;
}

.info-fade-enter-from {
  opacity: 0;
  transform: translate(-50%, calc(-100% - 4px));
}

.info-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, calc(-100% - 24px));
}

/* 地图说明文字 */
.map-caption {
  position: absolute;
  left: 16px;
  bottom: 14px;
  padding: 5px 12px;
  background: linear-gradient(135deg, rgba(10, 22, 40, 0.9), rgba(4, 10, 24, 0.92));
  color: var(--hud-cyan);
  font-size: 11px;
  border-radius: 2px;
  letter-spacing: 1px;
  font-family: -apple-system, 'PingFang SC', sans-serif;
  pointer-events: none;
  z-index: 4;
  border: 1px solid var(--hud-border);
  text-shadow: 0 0 6px rgba(0, 212, 255, 0.5);
  box-shadow: 0 0 12px var(--hud-glow);
  clip-path: polygon(0 0, calc(100% - 6px) 0, 100% 6px, 100% 100%, 6px 100%, 0 calc(100% - 6px));
}

.warningPulse {
  animation: warningPulse 1.5s ease-in-out infinite;
  transform-origin: center;
  transform-box: fill-box;
}

@keyframes warningPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* 筛选按钮 */
.filter-toggle-btn {
  position: absolute;
  top: 16px;
  left: 180px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: linear-gradient(135deg, rgba(10, 22, 40, 0.9), rgba(4, 10, 24, 0.92));
  border: 1px solid var(--hud-border);
  border-radius: 2px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
  color: var(--hud-text);
  cursor: pointer;
  box-shadow: 0 0 0 1px rgba(0, 212, 255, 0.06), 0 2px 12px rgba(0, 0, 0, 0.5), 0 0 14px var(--hud-glow);
  z-index: 10;
  transition: all 0.2s;
  backdrop-filter: blur(4px);
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
}

.filter-toggle-btn:hover {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.18), rgba(0, 212, 255, 0.08));
  border-color: rgba(0, 212, 255, 0.6);
  color: var(--hud-cyan);
  box-shadow: 0 0 18px rgba(0, 212, 255, 0.45);
}

.filter-toggle-btn.active {
  background: linear-gradient(135deg, var(--hud-cyan) 0%, #0099cc 100%);
  color: #04101f;
  border-color: rgba(0, 212, 255, 0.7);
  box-shadow: 0 0 18px rgba(0, 212, 255, 0.55);
}

.filter-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--hud-magenta);
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 2px;
  box-shadow: 0 0 10px rgba(255, 46, 159, 0.7);
}

.filter-toggle-btn.active .filter-badge {
  background: var(--hud-magenta);
  color: white;
}

/* 筛选面板 */
.filter-wrapper {
  position: absolute;
  top: 60px;
  left: 16px;
  z-index: 20;
  max-width: 400px;
}

/* 筛选动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}

/* 聚合模式提示 */
.cluster-hint {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  color: var(--hud-cyan);
  font-size: 13px;
  letter-spacing: 0.5px;
  border-radius: 2px;
  z-index: 10;
  backdrop-filter: blur(8px);
  text-shadow: 0 0 6px rgba(0, 212, 255, 0.5);
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 图例 */
.legend {
  position: absolute;
  top: 16px;
  left: 16px;
  border-radius: 2px;
  padding: 16px 18px 14px;
  z-index: 10;
  min-width: 150px;
  backdrop-filter: blur(8px);
}

.legend-section {
  margin-bottom: 12px;
  position: relative;
  z-index: 2;
}

.legend-section:last-child {
  margin-bottom: 0;
}

.legend-section:first-child {
  padding-bottom: 10px;
  border-bottom: 1px dashed rgba(0, 212, 255, 0.28);
}

.legend-title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--hud-cyan);
  margin-bottom: 8px;
  text-shadow: 0 0 6px rgba(0, 212, 255, 0.5);
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-title::before {
  content: '';
  width: 3px;
  height: 12px;
  background: var(--hud-cyan);
  box-shadow: 0 0 6px var(--hud-cyan);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--hud-text);
  margin-bottom: 4px;
}

.legend-item:last-child {
  margin-bottom: 0;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid rgba(0, 212, 255, 0.6);
  box-shadow: 0 0 6px currentColor;
}

.legend-icon {
  font-size: 14px;
}

/* 统计栏 */
.stats-bar {
  position: absolute;
  top: 16px;
  right: 16px;
  border-radius: 2px;
  padding: 12px 18px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 14px;
  backdrop-filter: blur(8px);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 50px;
  position: relative;
  z-index: 2;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  font-family: 'DIN Alternate', 'Bahnschrift', 'Consolas', monospace;
  letter-spacing: 1px;
  color: var(--hud-cyan);
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.6);
}

.stat-value.danger {
  color: var(--hud-magenta);
  text-shadow: 0 0 10px rgba(255, 46, 159, 0.7);
}

.stat-value.warning {
  color: var(--hud-amber);
  text-shadow: 0 0 10px rgba(251, 191, 36, 0.6);
}

.stat-label {
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--hud-text-dim);
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: linear-gradient(transparent, rgba(0, 212, 255, 0.5), transparent);
  position: relative;
  z-index: 2;
}

/* 缩放提示 */
.zoom-hint {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: linear-gradient(135deg, rgba(10, 22, 40, 0.9), rgba(4, 10, 24, 0.92));
  border: 1px solid var(--hud-border);
  border-radius: 2px;
  font-size: 12px;
  letter-spacing: 0.5px;
  font-family: 'DIN Alternate', 'Bahnschrift', 'Consolas', monospace;
  color: var(--hud-cyan);
  box-shadow: 0 0 0 1px rgba(0, 212, 255, 0.06), 0 2px 12px rgba(0, 0, 0, 0.5), 0 0 14px var(--hud-glow);
  z-index: 10;
  backdrop-filter: blur(4px);
  text-shadow: 0 0 6px rgba(0, 212, 255, 0.5);
  clip-path: polygon(0 0, calc(100% - 7px) 0, 100% 7px, 100% 100%, 7px 100%, 0 calc(100% - 7px));
}

.zoom-reset-btn {
  padding: 2px 8px;
  background: rgba(0, 212, 255, 0.18);
  border: 1px solid rgba(0, 212, 255, 0.5);
  border-radius: 2px;
  color: var(--hud-cyan);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s ease-out;
}

.zoom-reset-btn:hover {
  background: rgba(0, 212, 255, 0.35);
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
}

/* 加载状态 */
.loading-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, rgba(10, 22, 40, 0.92) 0%, rgba(4, 10, 24, 0.96) 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  z-index: 100;
  border-radius: 2px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(0, 212, 255, 0.18);
  border-top-color: var(--hud-cyan);
  border-right-color: rgba(0, 212, 255, 0.5);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  box-shadow: 0 0 14px rgba(0, 212, 255, 0.5);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-overlay span {
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--hud-cyan);
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.6);
}

/* 错误提示 */
.error-toast {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: linear-gradient(135deg, rgba(40, 10, 24, 0.95), rgba(20, 4, 12, 0.96));
  border: 1px solid rgba(255, 46, 159, 0.6);
  color: var(--hud-magenta);
  padding: 12px 22px;
  border-radius: 2px;
  font-size: 14px;
  letter-spacing: 0.5px;
  z-index: 100;
  text-shadow: 0 0 8px rgba(255, 46, 159, 0.6);
  box-shadow: 0 0 24px rgba(255, 46, 159, 0.3), 0 8px 28px rgba(0, 0, 0, 0.6);
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
}
</style>
