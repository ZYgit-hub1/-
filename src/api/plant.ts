import { request } from '@/utils/request'
import type { PowerPlant, PaginationParams } from '@/types'

// 电厂列表参数
export interface PlantListParams extends PaginationParams {
  type?: PowerPlant['type']
  status?: PowerPlant['status']
  keyword?: string
}

// 电厂列表响应
export interface PlantListResponse {
  list: PowerPlant[]
  total: number
}

// 获取电厂列表
export function getPlantList(params: PlantListParams) {
  return request.get<PlantListResponse>('/plants', params)
}

// 获取电厂详情
export function getPlantDetail(id: string) {
  return request.get<PowerPlant>(`/plants/${id}`)
}

// 获取电厂统计数据
export function getPlantStats() {
  return request.get<{
    totalPlants: number
    normalPlants: number
    warningPlants: number
    dangerPlants: number
    offlinePlants: number
  }>('/plants/stats')
}

// 创建电厂
export function createPlant(data: Partial<PowerPlant>) {
  return request.post<PowerPlant>('/plants', data)
}

// 更新电厂
export function updatePlant(id: string, data: Partial<PowerPlant>) {
  return request.put<PowerPlant>(`/plants/${id}`, data)
}

// 删除电厂
export function deletePlant(id: string) {
  return request.delete<void>(`/plants/${id}`)
}

// 更新电厂状态
export function updatePlantStatus(id: string, status: PowerPlant['status']) {
  return request.patch<PowerPlant>(`/plants/${id}/status`, { status })
}

// 获取电厂地图标记
export function getPlantMarkers() {
  return request.get<PowerPlant[]>('/plants/markers')
}
