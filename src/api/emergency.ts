import { request } from '@/utils/request'
import type { EmergencyResponse, PaginationParams } from '@/types'

// 应急响应参数
export interface EmergencyListParams extends PaginationParams {
  status?: EmergencyResponse['status']
  planLevel?: string
}

// 获取应急响应预案
export function getEmergencyPlan(plantId: string) {
  return request.get<EmergencyResponse>(`/emergency/${plantId}/plan`)
}

// 获取所有应急响应
export function getEmergencyList(params?: EmergencyListParams) {
  return request.get<EmergencyResponse[]>('/emergency/list', params)
}

// 激活应急响应
export function activateEmergency(plantId: string, data: {
  commander?: string
  contactPhone?: string
  measures?: string[]
}) {
  return request.post<EmergencyResponse>(`/emergency/${plantId}/activate`, data)
}

// 更新应急响应
export function updateEmergency(plantId: string, data: Partial<EmergencyResponse>) {
  return request.put<EmergencyResponse>(`/emergency/${plantId}`, data)
}

// 结束应急响应
export function endEmergency(plantId: string, remark?: string) {
  return request.patch<EmergencyResponse>(`/emergency/${plantId}/end`, { remark })
}

// 获取应急联系人
export function getEmergencyContacts(plantId: string) {
  return request.get<{
    name: string
    phone: string
    role: string
  }[]>(`/emergency/${plantId}/contacts`)
}

// 获取应急演练记录
export function getEmergencyDrills(plantId: string) {
  return request.get<{
    id: string
    date: string
    scenario: string
    participants: number
    result: 'success' | 'failed' | 'partial'
    remark?: string
  }[]>(`/emergency/${plantId}/drills`)
}
