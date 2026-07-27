import { request } from '@/utils/request'
import type { Alarm, AlarmLevel, AlarmStatus, PaginationParams } from '@/types'

// 报警列表参数
export interface AlarmListParams extends PaginationParams {
  level?: AlarmLevel
  status?: AlarmStatus
  plantId?: string
  startTime?: string
  endTime?: string
  keyword?: string
}

// 报警列表响应
export interface AlarmListResponse {
  list: Alarm[]
  total: number
  unread: number
}

// 获取报警列表
export function getAlarmList(params: AlarmListParams) {
  return request.get<AlarmListResponse>('/alarms', params)
}

// 获取报警详情
export function getAlarmDetail(id: string) {
  return request.get<Alarm>(`/alarms/${id}`)
}

// 确认报警
export function confirmAlarm(id: string, handler?: string, remark?: string) {
  return request.patch<Alarm>(`/alarms/${id}/confirm`, { handler, remark })
}

// 批量确认报警
export function confirmAlarms(ids: string[], handler?: string) {
  return request.patch<{ count: number }>('/alarms/confirm', { ids, handler })
}

// 解决报警
export function resolveAlarm(id: string, remark?: string) {
  return request.patch<Alarm>(`/alarms/${id}/resolve`, { remark })
}

// 批量解决报警
export function resolveAlarms(ids: string[], remark?: string) {
  return request.patch<{ count: number }>('/alarms/resolve', { ids, remark })
}

// 获取未读报警数量
export function getUnreadCount() {
  return request.get<{ count: number }>('/alarms/unread/count')
}

// 获取紧急报警
export function getEmergencyAlarms() {
  return request.get<Alarm[]>('/alarms/emergency')
}

// 获取最新报警
export function getLatestAlarms(limit?: number) {
  return request.get<Alarm[]>('/alarms/latest', { limit })
}

// 获取报警统计
export function getAlarmStats(params?: { startTime?: string; endTime?: string }) {
  return request.get<{
    total: number
    byLevel: Record<AlarmLevel, number>
    byStatus: Record<AlarmStatus, number>
    byPlant: { plantId: string; plantName: string; count: number }[]
  }>('/alarms/stats', params)
}

// 导出报警记录
export function exportAlarms(params: AlarmListParams) {
  return request.download('/alarms/export', params, `报警记录_${new Date().toISOString().slice(0, 10)}.xlsx`)
}
