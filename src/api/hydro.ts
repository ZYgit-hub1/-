import { request } from '@/utils/request'
import type { HydroData, AlertLevel, PaginationParams } from '@/types'

// 水文列表参数
export interface HydroListParams extends PaginationParams {
  plantId?: string
  alertLevel?: AlertLevel
  startTime?: string
  endTime?: string
}

// 水文列表响应
export interface HydroListResponse {
  list: HydroData[]
  total: number
}

// 获取水文数据
export function getHydroData(plantId: string) {
  return request.get<HydroData>(`/hydro/${plantId}`)
}

// 获取实时水文数据
export function getHydroRealTime(plantId: string) {
  return request.get<HydroData>(`/hydro/${plantId}/realtime`)
}

// 获取水文历史数据
export function getHydroHistory(
  plantId: string,
  startTime: string,
  endTime: string
) {
  return request.get<HydroData[]>('/hydro/history', {
    plantId,
    startTime,
    endTime
  })
}

// 获取水文统计
export function getHydroStats(plantId: string, days?: number) {
  return request.get<{
    avgWaterLevel: number
    maxWaterLevel: number
    minWaterLevel: number
    avgFlowRate: number
    maxFlowRate: number
    currentAlertLevel: AlertLevel
  }>(`/hydro/${plantId}/stats`, { days })
}

// 批量获取水文数据
export function getHydroBatch(plantIds: string[]) {
  return request.get<Record<string, HydroData>>('/hydro/batch', { plantIds })
}

// 获取告警阈值
export function getHydroThresholds(plantId: string) {
  return request.get<{
    watchLevel: number
    warningLevel: number
    floodLevel: number
  }>(`/hydro/${plantId}/thresholds`)
}

// 更新告警阈值
export function updateHydroThresholds(
  plantId: string,
  thresholds: {
    watchLevel?: number
    warningLevel?: number
    floodLevel?: number
  }
) {
  return request.put(`/hydro/${plantId}/thresholds`, thresholds)
}
