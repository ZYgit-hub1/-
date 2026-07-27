import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { usePlantStore } from '@/stores/plant'
import type { PowerPlant, WarningLevel } from '@/types'

/**
 * 兼容旧 API 的 composable 适配器
 * 内部代理到 usePlantStore，保持原有 usePlants() 用法不变
 */
export function usePlants() {
  const plantStore = usePlantStore()
  const { plants, filteredPlants, loading } = storeToRefs(plantStore)

  const selectedWarningLevel = ref<WarningLevel | 'all'>('all')
  const selectedType = ref<string>('all')

  // 加载所有电厂（代理到 store）
  const loadPlants = async () => {
    await plantStore.fetchPlants()
  }

  // 按预警等级筛选（基于 store 的 filteredPlants 再叠加本地过滤）
  const localFilteredPlants = computed(() => {
    let result = plants.value

    if (selectedWarningLevel.value !== 'all') {
      result = result.filter(p => p.warningLevel === selectedWarningLevel.value)
    }

    if (selectedType.value !== 'all') {
      result = result.filter(p => p.type === selectedType.value)
    }

    return result
  })

  // 获取单个电厂（优先从 store 中查找，缺失时拉取详情）
  const getPlantById = (id: string) => {
    return plants.value.find(p => p.id === id)
  }

  // 设置预警等级筛选
  const setWarningLevelFilter = (level: WarningLevel | 'all') => {
    selectedWarningLevel.value = level
  }

  // 设置类型筛选
  const setTypeFilter = (type: string) => {
    selectedType.value = type
  }

  // 重置筛选
  const resetFilters = () => {
    selectedWarningLevel.value = 'all'
    selectedType.value = 'all'
  }

  return {
    plants,
    filteredPlants: localFilteredPlants,
    loading,
    selectedWarningLevel,
    selectedType,
    loadPlants,
    getPlantById,
    setWarningLevelFilter,
    setTypeFilter,
    resetFilters
  }
}
