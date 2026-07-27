/**
 * 高德地图 Vue 3 Composable
 * 支持：初始化地图、添加标记点、监听点击事件、地图交互
 */

import { ref, onUnmounted, type Ref } from 'vue'

// 高德地图类型声明
declare global {
  interface Window {
    AMap: AMapNamespace
  }
}

interface AMapNamespace {
  Map: new (el: string | HTMLElement, options?: MapOptions) => AMapInstance
  Marker: new (options?: MarkerOptions) => AMapMarker
  InfoWindow: new (options?: InfoWindowOptions) => AMapInfoWindow
  Pixel: new (x: number, y: number) => AMapPixel
  ToolBar: new (options?: ToolBarOptions) => AMapToolBar
  Scale: new (options?: ScaleOptions) => AMapScale
  Bounds: new (sw: LngLat, ne: LngLat) => AMapBounds
  LabelMarker: new (options?: LabelMarkerOptions) => AMapLabelMarker
  Text: new (options?: TextOptions) => AMapText
  Circle: new (options?: CircleOptions) => AMapCircle
  DistrictSearch: new (options?: DistrictSearchOptions) => AMapDistrictSearch
  Geocoder: new (options?: GeocoderOptions) => AMapGeocoder
  Transfer: new (options?: TransferOptions) => AMapTransfer
  Driving: new (options?: DrivingOptions) => AMapDriving
  Walking: new (options?: WalkingOptions) => AMapWalking
  GeometryUtil: any
  LngLat: new (lng: number, lat: number) => AMapLngLat
}

interface AMapInstance {
  setCenter(lnglat: LngLatInput): void
  getZoom(): number
  setZoom(zoom: number): void
  getCenter(): AMapLngLat
  panTo(lnglat: LngLatInput): void
  setFitView(overlays?: any[]): void
  add(overlay: any): void
  remove(overlay: any): void
  clearMap(): void
  on(event: string, handler: (e: MapEvent) => void): void
  off(event: string, handler: (e: MapEvent) => void): void
  destroy(): void
  getAllOverlays(type?: string): any[]
}

interface AMapMarker {
  setPosition(position: LngLatInput): void
  getPosition(): AMapLngLat | undefined
  setContent(content: string | HTMLElement): void
  setTitle(title: string): void
  setExtData(data: any): void
  getExtData(): any
  setIcon(icon: any): void
  setLabel(label: { content: string; direction?: string; offset?: [number, number] }): void
  setOffset(offset: AMapPixel): void
  setMap(map: AMapInstance | null): void
  setClickable(flag: boolean): void
  setDraggable(flag: boolean): void
  on(event: string, handler: (e: MarkerEvent) => void): void
  off(event: string, handler: (e: MarkerEvent) => void): void
  getTitle(): string
}

interface AMapInfoWindow {
  open(map: AMapInstance, position: LngLatInput): void
  close(): void
  setContent(content: string | HTMLElement): void
  setSize(width: number, height: number): void
  setPosition(position: LngLatInput): void
  getIsOpen(): boolean
}

interface AMapText extends AMapMarker {
  setText(text: string): void
}

interface AMapCircle {
  setCenter(center: LngLatInput): void
  getCenter(): AMapLngLat
  setRadius(radius: number): void
  getRadius(): number
  setOptions(options: CircleOptions): void
  contains(point: LngLatInput): boolean
  setMap(map: AMapInstance | null): void
}

interface AMapLngLat {
  lng: number
  lat: number
  distance(lnglat: AMapLngLat): number
  equals(lnglat: AMapLngLat): boolean
}

interface AMapBounds {
  contains(lnglat: LngLatInput): boolean
  getSouthWest(): AMapLngLat
  getNorthEast(): AMapLngLat
}

interface AMapToolBar {}
interface AMapScale {}
interface AMapDistrictSearch {
  search(name: string, callback: (status: string, result: any) => void): void
}
interface AMapGeocoder {
  getAddress(lnglat: LngLatInput, callback: (status: string, result: any) => void): void
  getLocation(address: string, callback: (status: string, result: any) => void): void
}
interface AMapTransfer {}
interface AMapDriving {}
interface AMapWalking {}
interface AMapPixel {}
interface AMapLabelMarker {}

interface MapOptions {
  zoom?: number
  center?: LngLatInput
  viewMode?: '2D' | '3D'
  mapStyle?: string
  features?: string[]
  pitch?: number
  skyColor?: string
  buildingAnimation?: boolean
  showIndoorMap?: boolean
}

interface MarkerOptions {
  position?: LngLatInput
  content?: string | HTMLElement
  title?: string
  extData?: any
  icon?: any
  offset?: AMapPixel
  label?: { content: string; direction?: string; offset?: [number, number] }
  clickable?: boolean
  draggable?: boolean
  zIndex?: number
  angle?: number
  autoRotation?: boolean
  animation?: 'AMAP_ANIMATION_NONE' | 'AMAP_ANIMATION_DROP' | 'AMAP_ANIMATION_BOUNCE'
}

interface InfoWindowOptions {
  isCustom?: boolean
  closeWhenClickMap?: boolean
  content?: string | HTMLElement
  size?: { width: number; height: number }
  offset?: AMapPixel
  position?: LngLatInput
}

interface ToolBarOptions {
  position?: 'LT' | 'RT' | 'LB' | 'RB'
  offset?: AMapPixel
  ruler?: boolean
  noIpLocate?: boolean
  locate?: boolean
  liteStyle?: boolean
  direction?: boolean
}

interface ScaleOptions {
  position?: string
  offset?: AMapPixel
}

interface TextOptions extends MarkerOptions {
  text?: string
  style?: {
    fontSize?: string
    fontFamily?: string
    color?: string
    backgroundColor?: string
    borderColor?: string
    padding?: string
  }
}

interface CircleOptions {
  center?: LngLatInput
  radius?: number
  zIndex?: number
  bubble?: boolean
  cursor?: string
  strokeColor?: string
  strokeOpacity?: number
  strokeWeight?: number
  fillColor?: string
  fillOpacity?: number
  strokeStyle?: 'solid' | 'dashed'
  strokeDasharray?: number[]
  editable?: boolean
}

interface DistrictSearchOptions {
  level?: string
  extensions?: 'base' | 'all'
  subdistrict?: number
}

interface GeocoderOptions {
  radius?: number
  extensions?: 'base' | 'all'
}

interface TransferOptions {
  city?: string
  cityd?: string
  panel?: string | HTMLElement
  map?: AMapInstance
}

interface DrivingOptions {
  city?: string
  cityd?: string
}

interface WalkingOptions {
  city?: string
}

interface MapEvent {
  lnglat: AMapLngLat
  pixel: AMapPixel
  type: string
}

interface MarkerEvent {
  lnglat: AMapLngLat
  pixel: AMapPixel
  target: AMapMarker
}

type LngLatInput = [number, number] | AMapLngLat

// 配置
// 注:
//   1. 高德 JS API Key 必须为「Web 端 (JS API)」类型的 key，
//      申请地址: https://console.amap.com/dev/key/app
//   2. 在 .env.development / .env.production 中配置 VITE_AMAP_KEY；
//      若 key 缺失或仍为占位符，将自动回退到 SVG 占位地图（不影响主流程）。
const RAW_AMAP_KEY = (import.meta.env.VITE_AMAP_KEY as string | undefined) ?? ''
const AMAP_KEY = RAW_AMAP_KEY.trim()
export const AMAP_KEY_VALID =
  AMAP_KEY.length > 0 &&
  !AMAP_KEY.startsWith('__REPLACE_') &&
  AMAP_KEY !== 'YOUR_AMAP_KEY' &&
  AMAP_KEY !== 'your_amap_js_api_key_here'

const AMAP_CONFIG = {
  key: AMAP_KEY,
  version: '2.0',
  plugins: [
    'AMap.ToolBar',
    'AMap.Scale',
    'AMap.InfoWindow',
    'AMap.DistrictSearch',
    'AMap.Geocoder',
    'AMap.Transfer',
    'AMap.Driving',
    'AMap.Walking'
  ]
}

export interface MapMarker {
  marker: AMapMarker
  id: string
  data?: any
}

export interface AmapOptions {
  zoom?: number
  center?: [number, number]
  viewMode?: '2D' | '3D'
  mapStyle?: string
  enableAutoResize?: boolean
  showLabel?: boolean
}

export interface UseAmapReturn {
  // 状态
  map: Ref<AMapInstance | null>
  isLoaded: Ref<boolean>
  isPlaceholder: Ref<boolean>
  error: Ref<string | null>
  markers: Ref<MapMarker[]>

  // 地图操作
  initMap: (containerId: string, options?: AmapOptions) => Promise<AMapInstance | null>
  destroy: () => void

  // 标记操作
  addMarker: (options: AddMarkerOptions) => MapMarker | null
  addMarkers: (markers: AddMarkerOptions[]) => MapMarker[]
  removeMarker: (id: string) => void
  clearMarkers: () => void
  setMarkers: (markers: AddMarkerOptions[]) => void

  // 信息窗口
  openInfoWindow: (content: string | HTMLElement, position: [number, number]) => AMapInfoWindow | null
  closeInfoWindow: () => void

  // 事件监听
  onClick: (callback: (e: MapEvent) => void) => () => void
  onMarkerClick: (callback: (marker: MapMarker, e: MarkerEvent) => void) => () => void
  onZoomChange: (callback: (zoom: number) => void) => () => void
  onCenterChange: (callback: (center: AMapLngLat) => void) => () => void

  // 地图控制
  setCenter: (position: [number, number]) => void
  setZoom: (zoom: number) => void
  panTo: (position: [number, number]) => void
  setFitView: (padding?: number | [number, number, number, number]) => void
}

export interface AddMarkerOptions {
  id?: string
  position: [number, number]
  title?: string
  content?: string | HTMLElement
  icon?: string | { size: { width: number; height: number }; image: string }
  label?: { content: string; direction?: 'top' | 'bottom' | 'left' | 'right'; offset?: [number, number] }
  extData?: any
  draggable?: boolean
  clickable?: boolean
  zIndex?: number
  animation?: 'AMAP_ANIMATION_NONE' | 'AMAP_ANIMATION_DROP' | 'AMAP_ANIMATION_BOUNCE'
}

/**
 * 高德地图 Composable
 */
export function useAmap(): UseAmapReturn {
  const map = ref<AMapInstance | null>(null)
  const isLoaded = ref(false)
  const isPlaceholder = ref(false)
  const error = ref<string | null>(null)
  const markers = ref<MapMarker[]>([])
  const infoWindow = ref<AMapInfoWindow | null>(null)
  const eventHandlers: Map<string, (e: any) => void> = new Map()

  // 生成唯一ID
  const generateId = () => `marker_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`

  // 加载高德地图脚本
  const loadScript = (): Promise<void> => {
    return new Promise((resolve, reject) => {
      // 如果已加载，直接返回
      if (window.AMap) {
        resolve()
        return
      }

      // 检查是否正在加载
      const existingScript = document.querySelector('script[src*="webapi.amap.com"]')
      if (existingScript) {
        const checkLoaded = setInterval(() => {
          if (window.AMap) {
            clearInterval(checkLoaded)
            resolve()
          }
        }, 100)
        return
      }

      const script = document.createElement('script')
      const pluginUrl = AMAP_CONFIG.plugins.map(p => `plugin=${p}`).join('&')
      script.src = `https://webapi.amap.com/maps?v=${AMAP_CONFIG.version}&key=${AMAP_CONFIG.key}&${pluginUrl}`
      script.async = true
      script.charset = 'utf-8'

      script.onload = () => resolve()
      script.onerror = () => reject(new Error('高德地图加载失败'))

      document.head.appendChild(script)
    })
  }

  // 初始化地图
  const initMap = async (containerId: string, options?: AmapOptions): Promise<AMapInstance | null> => {
    try {
      // 检查容器是否存在
      const container = document.getElementById(containerId)
      if (!container) {
        error.value = '地图容器不存在'
        return null
      }

      // 无有效 Key：直接进入占位模式（不抛错，避免影响页面其他功能）
      if (!AMAP_KEY_VALID) {
        isLoaded.value = true
        isPlaceholder.value = true
        error.value = null
        return null
      }

      // 加载地图脚本
      await loadScript()

      if (!window.AMap) {
        throw new Error('高德地图 API 加载失败')
      }

      // 地图配置
      const defaultOptions: MapOptions = {
        zoom: options?.zoom ?? 10,
        center: options?.center ?? [113.280637, 23.125178], // 广州
        viewMode: options?.viewMode ?? '2D',
        mapStyle: options?.mapStyle ?? 'amap://styles/normal',
        features: ['bg', 'road', 'building'],
        enableAutoResize: true
      }

      // 创建地图实例
      const mapInstance = new window.AMap.Map(containerId, {
        ...defaultOptions,
        ...options
      })

      // 添加控件
      mapInstance.addControl(new window.AMap.Scale())
      mapInstance.addControl(new window.AMap.ToolBar({ position: 'RB' }))

      map.value = mapInstance
      isLoaded.value = true
      isPlaceholder.value = false

      return mapInstance
    } catch (e) {
      error.value = e instanceof Error ? e.message : '地图初始化失败'
      console.error('地图初始化失败:', e)
      return null
    }
  }

  // 销毁地图
  const destroy = () => {
    // 清理事件监听
    eventHandlers.forEach((handler, key) => {
      const [eventType, markerId] = key.split(':')
      if (markerId && map.value) {
        const marker = markers.value.find(m => m.id === markerId)
        if (marker) {
          marker.marker.off(eventType, handler)
        }
      } else if (map.value) {
        map.value.off(eventType, handler)
      }
    })
    eventHandlers.clear()

    // 关闭信息窗口
    closeInfoWindow()

    // 清理标记
    markers.value.forEach(({ marker }) => {
      marker.setMap(null)
    })
    markers.value = []

    // 销毁地图
    if (map.value) {
      map.value.destroy()
      map.value = null
    }

    isLoaded.value = false
    isPlaceholder.value = false
  }

  // 添加单个标记
  const addMarker = (options: AddMarkerOptions): MapMarker | null => {
    if (!map.value || !window.AMap) return null

    const markerOptions: MarkerOptions = {
      position: options.position,
      title: options.title,
      content: options.content,
      extData: options.extData,
      clickable: options.clickable ?? true,
      draggable: options.draggable ?? false,
      zIndex: options.zIndex ?? 100,
      animation: options.animation ?? 'AMAP_ANIMATION_DROP'
    }

    // 自定义图标
    if (options.icon) {
      if (typeof options.icon === 'string') {
        markerOptions.icon = new window.AMap.Icon({
          image: options.icon,
          size: new window.AMap.Pixel(32, 32),
          imageSize: new window.AMap.Pixel(32, 32)
        })
      } else {
        markerOptions.icon = new window.AMap.Icon({
          image: options.icon.image,
          size: new window.AMap.Pixel(options.icon.size.width, options.icon.size.height),
          imageSize: new window.AMap.Pixel(options.icon.size.width, options.icon.size.height)
        })
      }
    }

    const marker = new window.AMap.Marker(markerOptions)

    // 添加标签
    if (options.label) {
      marker.setLabel({
        content: options.label.content,
        direction: options.label.direction as any,
        offset: options.label.offset ? new window.AMap.Pixel(options.label.offset[0], options.label.offset[1]) : undefined
      })
    }

    marker.setMap(map.value)

    const mapMarker: MapMarker = {
      marker,
      id: options.id || generateId(),
      data: options.extData
    }

    markers.value.push(mapMarker)
    return mapMarker
  }

  // 批量添加标记
  const addMarkers = (markerOptions: AddMarkerOptions[]): MapMarker[] => {
    return markerOptions
      .map(opt => addMarker(opt))
      .filter((m): m is MapMarker => m !== null)
  }

  // 移除单个标记
  const removeMarker = (id: string) => {
    const index = markers.value.findIndex(m => m.id === id)
    if (index !== -1) {
      markers.value[index].marker.setMap(null)
      markers.value.splice(index, 1)
    }
  }

  // 清空所有标记
  const clearMarkers = () => {
    markers.value.forEach(({ marker }) => {
      marker.setMap(null)
    })
    markers.value = []
  }

  // 设置标记（替换现有标记）
  const setMarkers = (markerOptions: AddMarkerOptions[]) => {
    clearMarkers()
    addMarkers(markerOptions)
  }

  // 打开信息窗口
  const openInfoWindow = (content: string | HTMLElement, position: [number, number]): AMapInfoWindow | null => {
    if (!map.value || !window.AMap) return null

    // 关闭已打开的信息窗口
    closeInfoWindow()

    infoWindow.value = new window.AMap.InfoWindow({
      content,
      closeWhenClickMap: true,
      size: new window.AMap.Pixel(250, 0)
    })

    infoWindow.value.open(map.value, position)
    return infoWindow.value
  }

  // 关闭信息窗口
  const closeInfoWindow = () => {
    if (infoWindow.value) {
      infoWindow.value.close()
      infoWindow.value = null
    }
  }

  // 地图点击事件
  const onClick = (callback: (e: MapEvent) => void): (() => void) => {
    if (!map.value) return () => {}

    const handler = (e: MapEvent) => callback(e)
    map.value.on('click', handler)
    eventHandlers.set(`click:`, handler)

    return () => {
      map.value?.off('click', handler)
      eventHandlers.delete(`click:`)
    }
  }

  // 标记点击事件
  const onMarkerClick = (callback: (marker: MapMarker, e: MarkerEvent) => void): (() => void) => {
    if (!map.value) return () => {}

    const handler = (e: MarkerEvent) => {
      const clickedMarker = markers.value.find(m => m.marker === e.target)
      if (clickedMarker) {
        callback(clickedMarker, e)
      }
    }

    map.value.on('click', handler)
    eventHandlers.set(`click:marker`, handler)

    return () => {
      map.value?.off('click', handler)
      eventHandlers.delete(`click:marker`)
    }
  }

  // 缩放级别变化事件
  const onZoomChange = (callback: (zoom: number) => void): (() => void) => {
    if (!map.value) return () => {}

    const handler = () => {
      callback(map.value!.getZoom())
    }

    map.value.on('zoomchange', handler)
    eventHandlers.set(`zoomchange:`, handler)

    return () => {
      map.value?.off('zoomchange', handler)
      eventHandlers.delete(`zoomchange:`)
    }
  }

  // 中心点变化事件
  const onCenterChange = (callback: (center: AMapLngLat) => void): (() => void) => {
    if (!map.value) return () => {}

    const handler = () => {
      callback(map.value!.getCenter())
    }

    map.value.on('moveend', handler)
    eventHandlers.set(`moveend:`, handler)

    return () => {
      map.value?.off('moveend', handler)
      eventHandlers.delete(`moveend:`)
    }
  }

  // 设置地图中心
  const setCenter = (position: [number, number]) => {
    map.value?.setCenter(position)
  }

  // 设置缩放级别
  const setZoom = (zoom: number) => {
    map.value?.setZoom(zoom)
  }

  // 移动到指定位置
  const panTo = (position: [number, number]) => {
    map.value?.panTo(position)
  }

  // 自动适配视野
  const setFitView = (padding?: number | [number, number, number, number]) => {
    if (!map.value) return

    if (padding !== undefined) {
      map.value.setFitView(undefined, false, padding)
    } else {
      map.value.setFitView()
    }
  }

  // 组件卸载时清理
  onUnmounted(() => {
    destroy()
  })

  return {
    map,
    isLoaded,
    isPlaceholder,
    error,
    markers,
    initMap,
    destroy,
    addMarker,
    addMarkers,
    removeMarker,
    clearMarkers,
    setMarkers,
    openInfoWindow,
    closeInfoWindow,
    onClick,
    onMapClick: onClick, // 别名，兼容旧调用方（PowerPlantMap.vue）
    onMarkerClick,
    onZoomChange,
    onCenterChange,
    setCenter,
    setZoom,
    panTo,
    setFitView
  }
}

// 电厂类型图标映射
export const PLANT_ICONS: Record<string, string> = {
  coal: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png',
  gas: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png',
  solar: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png',
  wind: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png',
  storage: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png'
}

// 预警等级颜色
export const WARNING_COLORS: Record<string, string> = {
  green: '#22d3ee',
  blue: '#00d4ff',
  yellow: '#fbbf24',
  orange: '#fb923c',
  red: '#ff2e9f'
}

export default useAmap
