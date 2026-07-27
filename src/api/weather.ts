import { request } from '@/utils/request'
import type { WeatherData, WeatherForecast } from '@/types'

// 获取气象数据
export function getWeather(plantId: string) {
  return request.get<WeatherData>(`/weather/${plantId}`)
}

// 获取实时气象数据
export function getWeatherRealTime(plantId: string) {
  return request.get<WeatherData>(`/weather/${plantId}/realtime`)
}

// 获取气象预报
export function getWeatherForecast(plantId: string, days?: number) {
  return request.get<WeatherForecast[]>(`/weather/${plantId}/forecast`, { days })
}

// 获取历史气象数据
export function getWeatherHistory(plantId: string, startTime: string, endTime: string) {
  return request.get<WeatherData[]>('/weather/history', {
    plantId,
    startTime,
    endTime
  })
}

// 获取气象预警
export function getWeatherAlerts(plantId: string) {
  return request.get<{
    level: string
    content: string
    startTime: string
    endTime?: string
  }[]>(`/weather/${plantId}/alerts`)
}

// 批量获取气象数据
export function getWeatherBatch(plantIds: string[]) {
  return request.get<Record<string, WeatherData>>('/weather/batch', { plantIds })
}
