import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  Alarm,
  AlarmStatus,
  AlarmLevel,
  Warning,
  WarningLevel,
  WarningType,
  DashboardStats
} from '@/types'
import { mockApi } from '@/mock/api'
import { MOCK_DATA } from '@/mock/data'

export const useAlarmStore = defineStore('alarm', () => {
  // ============================================================================
  // State
  // ============================================================================

  const alarms = ref<Alarm[]>([])
  const warnings = ref<Warning[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // ============================================================================
  // Getters
  // ============================================================================

  const unreadAlarms = computed(() => {
    return alarms.value.filter(a => a.status === 'unconfirmed')
  })

  const unreadCount = computed(() => {
    return unreadAlarms.value.length
  })

  const pendingAlarms = computed(() => {
    return alarms.value.filter(a => a.status === 'unconfirmed')
  })

  const activeAlarms = computed(() => {
    return alarms.value.filter(a => a.status !== 'resolved')
  })

  const alarmsByStatus = computed(() => {
    return {
      unconfirmed: alarms.value.filter(a => a.status === 'unconfirmed'),
      confirmed: alarms.value.filter(a => a.status === 'confirmed'),
      resolved: alarms.value.filter(a => a.status === 'resolved')
    }
  })

  const stats = computed<DashboardStats>(() => {
    const plants = MOCK_DATA.plants
    return {
      totalPlants: plants.length,
      normalPlants: plants.filter(p => p.status === 'normal').length,
      warningPlants: plants.filter(p => p.status === 'warning').length,
      dangerPlants: plants.filter(p => p.status === 'danger').length,
      totalAlarms: alarms.value.length,
      unreadAlarms: unreadCount.value,
      activeWarnings: warnings.value.filter(w => w.status === 'active').length
    }
  })

  // ============================================================================
  // Actions
  // ============================================================================

  /**
   * 获取报警列表
   * 调用 mockApi.getAlarms()，使用 pageSize=1000 一次性加载
   */
  const fetchAlarms = async (params?: {
    status?: AlarmStatus
    level?: AlarmLevel
    plantId?: string
    keyword?: string
  }) => {
    loading.value = true
    error.value = null

    try {
      const res = await mockApi.getAlarms({
        page: 1,
        pageSize: 1000,
        status: params?.status,
        level: params?.level,
        plantId: params?.plantId,
        keyword: params?.keyword
      })
      alarms.value = res.list
    } catch (e) {
      error.value = '获取报警列表失败'
      console.error('fetchAlarms error:', e)
    } finally {
      loading.value = false
    }
  }

  /**
   * 确认报警
   * 调用 mockApi.updateAlarmStatus(alarmId, 'confirmed')，
   * 成功后用返回的最新对象覆盖本地 state 中的对应报警。
   */
  const confirmAlarm = async (alarmId: string, remark?: string) => {
    const index = alarms.value.findIndex(a => a.id === alarmId)
    if (index === -1) {
      console.warn(`confirmAlarm: alarm ${alarmId} not found in local state`)
      return
    }

    try {
      const updated = await mockApi.updateAlarmStatus(alarmId, 'confirmed')

      // 用后端返回的最新对象覆盖本地数据
      const next: Alarm = {
        ...updated,
        // 若调用方传入备注则附加
        remark: remark || updated.remark
      }
      const newList = [...alarms.value]
      newList[index] = next
      alarms.value = newList
    } catch (e) {
      error.value = '确认报警失败'
      console.error('confirmAlarm error:', e)
      throw e
    }
  }

  /**
   * 处理报警（标记为已解决）
   * 保留原有行为，便于 UI 调用
   */
  const resolveAlarm = async (id: string, remark?: string) => {
    const index = alarms.value.findIndex(a => a.id === id)
    if (index === -1) return

    try {
      const updated = await mockApi.updateAlarmStatus(id, 'resolved')
      const next: Alarm = {
        ...updated,
        remark: remark || updated.remark
      }
      const newList = [...alarms.value]
      newList[index] = next
      alarms.value = newList
    } catch (e) {
      console.error('resolveAlarm error:', e)
      throw e
    }
  }

  /**
   * 获取预警列表
   * 调用 mockApi.getWarnings()
   */
  const fetchWarnings = async (params?: {
    level?: WarningLevel
    type?: WarningType
    plantId?: string
    status?: 'active' | 'expired' | 'cancelled'
  }) => {
    loading.value = true
    error.value = null

    try {
      const res = await mockApi.getWarnings({
        page: 1,
        pageSize: 1000,
        level: params?.level,
        type: params?.type,
        plantId: params?.plantId,
        status: params?.status
      })
      warnings.value = res.list
    } catch (e) {
      error.value = '获取预警列表失败'
      console.error('fetchWarnings error:', e)
    } finally {
      loading.value = false
    }
  }

  return {
    // State
    alarms,
    warnings,
    loading,
    error,

    // Getters
    unreadAlarms,
    unreadCount,
    pendingAlarms,
    activeAlarms,
    alarmsByStatus,
    stats,

    // Actions
    fetchAlarms,
    confirmAlarm,
    resolveAlarm,
    fetchWarnings
  }
})
