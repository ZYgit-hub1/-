import { ref } from 'vue'

declare global {
  interface Window {
    AMap: any
  }
}

export interface MapInstance {
  map: any
  AMap: any
}

const AMAP_KEY = 'YOUR_AMAP_KEY' // 替换为你的高德地图 API Key

export function useMap() {
  const mapInstance = ref<any>(null)
  const AMapInstance = ref<any>(null)
  const isLoaded = ref(false)
  const error = ref<string | null>(null)
  const isPlaceholder = ref(false)

  const renderPlaceholderMap = (containerId: string, options?: any): any => {
    const container = document.getElementById(containerId)
    if (!container) return null

    const plants = (window as any).__mapPlants || []
    const zoom = options?.zoom || 8
    const center = options?.center || [113.280637, 23.125178]

    container.innerHTML = `
      <div style="
        position: relative;
        width: 100%;
        height: 100%;
        background: linear-gradient(135deg, #1e3a8a 0%, #0c4a6e 50%, #075985 100%);
        overflow: hidden;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      ">
        <!-- 装饰背景网格 -->
        <svg style="position:absolute;inset:0;width:100%;height:100%;opacity:0.15;" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
              <path d="M 50 0 L 0 0 0 50" fill="none" stroke="white" stroke-width="0.5"/>
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)"/>
        </svg>

        <!-- 装饰光晕 -->
        <div style="position:absolute;top:20%;left:30%;width:300px;height:300px;background:radial-gradient(circle,rgba(56,189,248,0.3) 0%,transparent 70%);border-radius:50%;filter:blur(40px);"></div>
        <div style="position:absolute;bottom:20%;right:20%;width:250px;height:250px;background:radial-gradient(circle,rgba(34,197,94,0.2) 0%,transparent 70%);border-radius:50%;filter:blur(40px);"></div>

        <!-- 顶部状态条 -->
        <div style="position:absolute;top:16px;left:50%;transform:translateX(-50%);background:rgba(0,0,0,0.4);backdrop-filter:blur(8px);color:white;padding:8px 20px;border-radius:24px;font-size:13px;display:flex;align-items:center;gap:10px;border:1px solid rgba(255,255,255,0.15);z-index:10;">
          <span style="width:8px;height:8px;background:#22c55e;border-radius:50%;box-shadow:0 0 8px #22c55e;animation:pulse 2s infinite;"></span>
          <span>地图占位视图（演示模式）· 广东省电厂分布</span>
        </div>

        <!-- 电厂标记容器 -->
        <div id="placeholder-plants" style="position:absolute;inset:0;"></div>

        <!-- 右下角图例 -->
        <div style="position:absolute;bottom:16px;right:16px;background:rgba(255,255,255,0.95);backdrop-filter:blur(8px);padding:12px;border-radius:8px;box-shadow:0 4px 12px rgba(0,0,0,0.2);font-size:12px;">
          <div style="font-weight:600;margin-bottom:6px;color:#1f2937;">缩放级别 ${zoom}</div>
          <div style="color:#6b7280;">中心: ${center[0].toFixed(2)}, ${center[1].toFixed(2)}</div>
        </div>

        <!-- 左下角统计 -->
        <div style="position:absolute;bottom:16px;left:16px;background:rgba(255,255,255,0.95);backdrop-filter:blur(8px);padding:12px 16px;border-radius:8px;box-shadow:0 4px 12px rgba(0,0,0,0.2);">
          <div style="font-size:11px;color:#6b7280;margin-bottom:2px;">显示电厂数</div>
          <div style="font-size:24px;font-weight:700;color:#0ea5e9;">${plants.length}</div>
        </div>

        <style>
          @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.5} }
          .placeholder-plant {
            position: absolute;
            transform: translate(-50%, -50%);
            cursor: pointer;
            transition: transform 0.2s;
            z-index: 5;
          }
          .placeholder-plant:hover {
            transform: translate(-50%, -50%) scale(1.3);
          }
          .placeholder-plant-dot {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            border: 3px solid white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 16px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.4);
          }
          .placeholder-plant-pulse {
            position: absolute;
            inset: -8px;
            border-radius: 50%;
            border: 2px solid currentColor;
            opacity: 0.6;
            animation: ripple 2s infinite;
          }
          @keyframes ripple {
            0% { transform: scale(0.8); opacity: 0.6; }
            100% { transform: scale(2); opacity: 0; }
          }
        </style>
      </div>
    `

    // 渲染电厂标记
    requestAnimationFrame(() => {
      const plantsContainer = document.getElementById('placeholder-plants')
      if (!plantsContainer) return

      const warningColors: Record<string, string> = {
        green: '#10b981',
        blue: '#3b82f6',
        yellow: '#f59e0b',
        orange: '#f97316',
        red: '#ef4444'
      }

      const icons: Record<string, string> = {
        coal: '🏭',
        gas: '⚡',
        solar: '☀️',
        wind: '🌬️',
        storage: '🔋'
      }

      plants.forEach((plant: any) => {
        const x = 50 + (plant.location.lng - 113.0) * 30
        const y = 50 - (plant.location.lat - 22.5) * 30
        const color = warningColors[plant.warningLevel || 'green']
        const icon = icons[plant.type] || '🏭'

        const dot = document.createElement('div')
        dot.className = 'placeholder-plant'
        dot.style.left = `${x}%`
        dot.style.top = `${y}%`
        dot.style.color = color
        dot.innerHTML = `
          <div class="placeholder-plant-pulse"></div>
          <div class="placeholder-plant-dot" style="background:${color};">
            ${icon}
          </div>
        `

        dot.addEventListener('click', () => {
          window.dispatchEvent(new CustomEvent('placeholder-marker-click', {
            detail: { plant, x, y }
          }))
        })

        plantsContainer.appendChild(dot)
      })
    })

    return {
      setCenter: () => {},
      setZoom: () => {},
      destroy: () => { container.innerHTML = '' },
      on: () => {},
      getZoom: () => zoom,
      addControl: () => {}
    }
  }

  const loadMap = async (containerId: string, options?: any): Promise<any> => {
    return new Promise((resolve, reject) => {
      if (AMAP_KEY === 'YOUR_AMAP_KEY' || !AMAP_KEY) {
        // 使用占位地图
        console.warn('⚠️ 未配置高德地图 Key，使用占位地图视图')
        const map = renderPlaceholderMap(containerId, options)
        if (map) {
          mapInstance.value = map
          isPlaceholder.value = true
          isLoaded.value = true
          resolve(map)
        } else {
          reject(new Error('占位地图渲染失败'))
        }
        return
      }

      if (window.AMap && mapInstance.value) {
        resolve(mapInstance.value)
        return
      }

      const script = document.createElement('script')
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.ToolBar,AMap.Scale,AMap.InfoWindow`
      script.async = true

      script.onload = () => {
        try {
          AMapInstance.value = window.AMap

          const defaultOptions = {
            zoom: 10,
            center: [113.280637, 23.125178],
            viewMode: '2D',
            mapStyle: 'amap://styles/normal'
          }

          const map = new window.AMap.Map(containerId, {
            ...defaultOptions,
            ...options
          })

          map.addControl(new window.AMap.Scale())
          map.addControl(new window.AMap.ToolBar({ position: 'RB' }))

          mapInstance.value = map
          isLoaded.value = true
          resolve(map)
        } catch (e) {
          error.value = '地图初始化失败'
          reject(e)
        }
      }

      script.onerror = () => {
        console.warn('高德地图脚本加载失败，自动切换到占位地图')
        const map = renderPlaceholderMap(containerId, options)
        if (map) {
          mapInstance.value = map
          isPlaceholder.value = true
          isLoaded.value = true
          resolve(map)
        } else {
          error.value = '地图加载失败'
          reject(new Error('地图加载失败'))
        }
      }

      document.head.appendChild(script)
    })
  }

  const addMarker = (options: {
    position: [number, number]
    content?: string
    extData?: any
    icon?: any
    offset?: [number, number]
  }) => {
    if (!mapInstance.value) return null
    if (isPlaceholder.value) return null

    const marker = new window.AMap.Marker({
      position: options.position,
      content: options.content,
      extData: options.extData,
      icon: options.icon,
      offset: options.offset || new window.AMap.Pixel(-13, -30)
    })

    marker.setMap(mapInstance.value)
    return marker
  }

  const clearMarkers = (markers: any[]) => {
    if (!mapInstance.value) return
    markers.forEach(marker => {
      if (marker && marker.setMap) marker.setMap(null)
    })
  }

  const createInfoWindow = (content: string, options?: any) => {
    if (!window.AMap) return null
    return new window.AMap.InfoWindow({ content, ...options })
  }

  const onZoomChange = (callback: (zoom: number) => void) => {
    if (!mapInstance.value || isPlaceholder.value) return
    mapInstance.value.on('zoomchange', () => {
      callback(mapInstance.value.getZoom())
    })
  }

  const onMapClick = (callback: (lnglat: any) => void) => {
    if (!mapInstance.value || isPlaceholder.value) return
    mapInstance.value.on('click', (e: any) => {
      callback(e.lnglat)
    })
  }

  const onMarkerClick = (callback: (marker: any) => void) => {
    if (!mapInstance.value || isPlaceholder.value) {
      window.addEventListener('placeholder-marker-click', (e: any) => {
        callback({
          getExtData: () => e.detail.plant
        })
      })
      return
    }
    mapInstance.value.on('click', (e: any) => {
      if (e.target instanceof window.AMap.Marker) {
        callback(e.target)
      }
    })
  }

  const setCenter = (position: [number, number]) => {
    if (!mapInstance.value || isPlaceholder.value) return
    mapInstance.value.setCenter(position)
  }

  const setZoom = (zoom: number) => {
    if (!mapInstance.value || isPlaceholder.value) return
    mapInstance.value.setZoom(zoom)
  }

  const destroy = () => {
    if (mapInstance.value) {
      if (!isPlaceholder.value && mapInstance.value.destroy) {
        mapInstance.value.destroy()
      } else {
        const container = document.getElementById('map-container')
        if (container) container.innerHTML = ''
      }
      mapInstance.value = null
      isLoaded.value = false
      isPlaceholder.value = false
    }
  }

  return {
    mapInstance,
    AMapInstance,
    isLoaded,
    error,
    isPlaceholder,
    loadMap,
    addMarker,
    clearMarkers,
    createInfoWindow,
    onZoomChange,
    onMapClick,
    onMarkerClick,
    setCenter,
    setZoom,
    destroy
  }
}