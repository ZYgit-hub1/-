import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { PowerPlant, WeatherData, HydroData } from '@/types'
import { mockApi, type PlantDetail } from '@/mock/api'

/** 单个电厂的详情缓存（包含历史气象和水文） */
export interface PlantDetailCache {
  weatherHistory: WeatherData[]
  hydroHistory: HydroData[]
  /** 缓存的快照时间 */
  fetchedAt: string
}

export const usePlantStore = defineStore('plant', () => {
  // ============================================================================
  // State
  // ============================================================================

  const plants = ref<PowerPlant[]>([])
  const selectedPlant = ref<PowerPlant | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  /** 每个电厂的详情（key 为 plantId） */
  const plantDetails = ref<Record<string, PlantDetailCache>>({})

  /** 详情加载态（key 为 plantId） */
  const detailLoading = ref<Record<string, boolean>>({})

  // Filters
  const filterWarningLevel = ref<string>('all')
  const filterType = ref<string>('all')
  /** 搜索关键字（按名称/类型/地址模糊匹配） */
  const searchTerm = ref<string>('')

  // ============================================================================
  // Getters
  // ============================================================================

  /**
   * 过滤后的电厂列表
   * 顺序：searchTerm → filterWarningLevel → filterType
   */
  const filteredPlants = computed(() => {
    let result = plants.value

    // 搜索关键字（名称 / 类型 / 地址）
    const kw = searchTerm.value.trim().toLowerCase()
    if (kw) {
      result = result.filter(
        p =>
          p.name.toLowerCase().includes(kw) ||
          p.type.toLowerCase().includes(kw) ||
          (p.address && p.address.toLowerCase().includes(kw))
      )
    }

    if (filterWarningLevel.value !== 'all') {
      result = result.filter(p => p.warningLevel === filterWarningLevel.value)
    }

    if (filterType.value !== 'all') {
      result = result.filter(p => p.type === filterType.value)
    }

    return result
  })

  const plantCountByType = computed(() => {
    const count = {
      coal: 0,
      gas: 0,
      solar: 0,
      wind: 0,
      storage: 0
    }

    plants.value.forEach(p => {
      count[p.type]++
    })

    return count
  })

  const plantCountByWarning = computed(() => {
    const count = {
      green: 0,
      blue: 0,
      yellow: 0,
      orange: 0,
      red: 0
    }

    plants.value.forEach(p => {
      if (p.warningLevel) {
        count[p.warningLevel]++
      }
    })

    return count
  })

  // ============================================================================
  // Actions
  // ============================================================================

  /**
   * 获取电厂列表
   * 调用 mockApi.getPlants()，挂分页参数 pageSize=1000 一次性加载全部
   */
  const fetchPlants = async () => {
    loading.value = true
    error.value = null

    try {
      const res = await mockApi.getPlants({ page: 1, pageSize: 1000 })
      plants.value = res.list
    } catch (e) {
      error.value = '获取电厂列表失败'
      console.error('fetchPlants error:', e)
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取电厂详情
   * 缓存气象与水文历史数据到 plantDetails[plantId]
   */
  const fetchPlantDetail = async (plantId: string) => {
    detailLoading.value = { ...detailLoading.value, [plantId]: true }
    error.value = null

    try {
      const detail: PlantDetail = await mockApi.getPlantDetail(plantId)

      // 更新基础信息到 selectedPlant
      const { weatherHistory, hydroHistory, ...plant } = detail
      selectedPlant.value = plant as PowerPlant

      // 缓存历史数据
      plantDetails.value = {
        ...plantDetails.value,
        [plantId]: {
          weatherHistory,
          hydroHistory,
          fetchedAt: new Date().toISOString()
        }
      }

      return detail
    } catch (e) {
      error.value = '获取电厂详情失败'
      console.error('fetchPlantDetail error:', e)
      return null
    } finally {
      detailLoading.value = { ...detailLoading.value, [plantId]: false }
    }
  }

  /**
   * 设置搜索关键字（用于 UI 输入框实时过滤）
   * 传空字符串可清除搜索
   */
  const setSearchTerm = (term: string) => {
    searchTerm.value = term
  }

  const setFilterWarningLevel = (level: string) => {
    filterWarningLevel.value = level
  }

  const setFilterType = (type: string) => {
    filterType.value = type
  }

  const resetFilters = () => {
    filterWarningLevel.value = 'all'
    filterType.value = 'all'
    searchTerm.value = ''
  }

  /** 获取已缓存的电厂历史数据（无缓存返回 undefined） */
  const getPlantHistory = (plantId: string) => plantDetails.value[plantId]

  /**
   * 从本地 state 查找电厂（同步）
   * 兼容旧 API: mockDataService.getPlantById
   */
  const getPlantById = (id: string): PowerPlant | undefined => {
    return plants.value.find(p => p.id === id)
  }

  /**
   * 获取电厂最近一条气象数据
   * 优先从缓存读取最新一条；无缓存时返回 null
   * 兼容旧 API: mockDataService.getWeatherData
   */
  const getWeatherData = (plantId: string): WeatherData | null => {
    const cache = plantDetails.value[plantId]
    if (!cache || cache.weatherHistory.length === 0) return null
    return cache.weatherHistory[cache.weatherHistory.length - 1]
  }

  /**
   * 获取电厂最近一条水文数据
   * 兼容旧 API: mockDataService.getHydroData
   */
  const getHydroData = (plantId: string): HydroData | null => {
    const cache = plantDetails.value[plantId]
    if (!cache || cache.hydroHistory.length === 0) return null
    return cache.hydroHistory[cache.hydroHistory.length - 1]
  }

  /** 清除指定电厂的详情缓存 */
  const clearPlantDetail = (plantId: string) => {
    const next = { ...plantDetails.value }
    delete next[plantId]
    plantDetails.value = next
  }

  return {
    // State
    plants,
    selectedPlant,
    loading,
    error,
    plantDetails,
    detailLoading,
    filterWarningLevel,
    filterType,
    searchTerm,

    // Getters
    filteredPlants,
    plantCountByType,
    plantCountByWarning,

    // Actions
    fetchPlants,
    fetchPlantDetail,
    setSearchTerm,
    setFilterWarningLevel,
    setFilterType,
    resetFilters,
    getPlantHistory,
    getPlantById,
    getWeatherData,
    getHydroData,
    clearPlantDetail
  }
})
