import { request } from '@/utils/request'
import type { DashboardStats } from '@/types'

// 获取仪表盘统计
export function getDashboardStats() {
  return request.get<DashboardStats>('/dashboard/stats')
}

// 获取实时告警概览
export function getDashboardAlerts() {
  return request.get<{
    emergency: number
    high: number
    medium: number
    low: number
    unconfirmed: number
  }>('/dashboard/alerts')
}

// 获取预警概览
export function getDashboardWarnings() {
  return request.get<{
    red: number
    orange: number
    yellow: number
    blue: number
    green: number
  }>('/dashboard/warnings')
}

// 获取电厂状态概览
export function getDashboardPlantStatus() {
  return request.get<{
    normal: number
    warning: number
    danger: number
    offline: number
  }>('/dashboard/plant-status')
}

// 获取气象概览
export function getDashboardWeather() {
  return request.get<{
    extremeTemp: { plantId: string; plantName: string; temp: number }[]
    highHumidity: { plantId: string; plantName: string; humidity: number }[]
    strongWind: { plantId: string; plantName: string; windSpeed: number }[]
    heavyRain: { plantId: string; plantName: string; rainfall: number }[]
  }>('/dashboard/weather')
}

// 获取水文概览
export function getDashboardHydro() {
  return request.get<{
    watchLevel: { plantId: string; plantName: string; waterLevel: number }[]
    warningLevel: { plantId: string; plantName: string; waterLevel: number }[]
    floodLevel: { plantId: string; plantName: string; waterLevel: number }[]
  }>('/dashboard/hydro')
}

// 获取最近活动
export function getRecentActivity(limit?: number) {
  return request.get<{
    type: 'alarm' | 'warning' | 'hydro' | 'weather'
    title: string
    content: string
    time: string
    plantId?: string
    plantName?: string
  }[]>('/dashboard/recent', { limit })
}
