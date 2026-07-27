import { request } from '@/utils/request'
import type { Warning, WarningLevel, WarningType, PaginationParams } from '@/types'

// 预警列表参数
export interface WarningListParams extends PaginationParams {
  level?: WarningLevel
  type?: WarningType
  plantId?: string
  status?: 'active' | 'expired' | 'cancelled'
  startTime?: string
  endTime?: string
}

// 预警列表响应
export interface WarningListResponse {
  list: Warning[]
  total: number
}

// 获取预警列表
export function getWarningList(params: WarningListParams) {
  return request.get<WarningListResponse>('/warnings', params)
}

// 获取预警详情
export function getWarningDetail(id: string) {
  return request.get<Warning>(`/warnings/${id}`)
}

// 获取活动预警
export function getActiveWarnings() {
  return request.get<Warning[]>('/warnings/active')
}

// 获取最新预警
export function getLatestWarnings(limit?: number) {
  return request.get<Warning[]>('/warnings/latest', { limit })
}

// 获取预警统计
export function getWarningStats() {
  return request.get<{
    total: number
    byLevel: Record<WarningLevel, number>
    byType: Record<WarningType, number>
    byPlant: { plantId: string; plantName: string; count: number }[]
  }>('/warnings/stats')
}

// 创建预警
export function createWarning(data: Partial<Warning>) {
  return request.post<Warning>('/warnings', data)
}

// 更新预警
export function updateWarning(id: string, data: Partial<Warning>) {
  return request.put<Warning>(`/warnings/${id}`, data)
}

// 取消预警
export function cancelWarning(id: string, reason?: string) {
  return request.patch<Warning>(`/warnings/${id}/cancel`, { reason })
}

// 批量取消预警
export function cancelWarnings(ids: string[], reason?: string) {
  return request.patch<{ count: number }>('/warnings/cancel', { ids, reason })
}

// 获取预警历史
export function getWarningHistory(plantId: string, days?: number) {
  return request.get<Warning[]>('/warnings/history', { plantId, days })
}
