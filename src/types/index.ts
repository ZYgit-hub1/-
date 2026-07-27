// 电厂信息
export interface PowerPlant {
  id: string
  name: string
  type: 'coal' | 'gas' | 'solar' | 'wind' | 'storage'
  location: {
    lat: number
    lng: number
  }
  status: 'normal' | 'warning' | 'danger' | 'offline'
  warningLevel?: WarningLevel
  capacity: number // 装机容量 MW
  address?: string
}

// 气象数据
export interface WeatherData {
  temp: number // 温度 °C
  humidity: number // 湿度 %
  windSpeed: number // 风速 m/s
  windDirection: number // 风向 °
  rainfall: number // 降雨量 mm
  pressure: number // 气压 hPa
  updateTime: string
  forecast?: WeatherForecast[]
}

export interface WeatherForecast {
  time: string
  temp: number
  weather: string
  windSpeed: number
}

// 水文数据
export interface HydroData {
  waterLevel: number // 水位 m
  flowRate: number // 流量 m³/s
  alertLevel: AlertLevel
  updateTime: string
  history?: HydroHistory[]
}

export interface HydroHistory {
  time: string
  waterLevel: number
  flowRate: number
}

export type AlertLevel = 'normal' | 'watch' | 'warning' | 'flood'

// 报警信息
export interface Alarm {
  id: string
  level: AlarmLevel
  status: AlarmStatus
  content: string
  plantId: string
  plantName: string
  triggerTime: string
  confirmTime?: string
  resolveTime?: string
  handler?: string
  remark?: string
}

export type AlarmLevel = 'emergency' | 'high' | 'medium' | 'low'
export type AlarmStatus = 'unconfirmed' | 'confirmed' | 'resolved'

// 预警信息
export interface Warning {
  id: string
  level: WarningLevel
  type: WarningType
  content: string
  plantId: string
  plantName: string
  startTime: string
  endTime?: string
  status: 'active' | 'expired' | 'cancelled'
}

export type WarningLevel = 'green' | 'blue' | 'yellow' | 'orange' | 'red'
export type WarningType = 'weather' | 'flood' | 'fire' | 'equipment' | 'other'

// 应急响应
export interface EmergencyResponse {
  planLevel: string
  activationTime?: string
  commander?: string
  contactPhone?: string
  measures: string[]
  status: 'standby' | 'activated' | 'ended'
}

// 统计数据
export interface DashboardStats {
  totalPlants: number
  normalPlants: number
  warningPlants: number
  dangerPlants: number
  totalAlarms: number
  unreadAlarms: number
  activeWarnings: number
}

// 分页参数
export interface PaginationParams {
  page: number
  pageSize: number
  total?: number
}

// 地图标记
export interface MapMarker {
  id: string
  position: [number, number]
  type: 'coal' | 'gas' | 'solar' | 'wind' | 'storage'
  warningLevel?: WarningLevel
  plant: PowerPlant
}

// 水文站
export interface HydroStation {
  id: string
  name: string
  /** 所属水系/河流 */
  river: string
  /** 所在城市 */
  city: string
  location: {
    lat: number
    lng: number
  }
  /** 警戒水位 (m) */
  warningLevel: number
  /** 保证水位 (m) */
  guaranteeLevel: number
  /** 历史最高水位 (m) */
  historicalMax: number
  /** 当前实时水位数据 */
  current?: HydroStationReading
}

// 水文站实时读数
export interface HydroStationReading {
  /** 水位 (m) */
  waterLevel: number
  /** 流量 (m³/s) */
  flowRate: number
  /** 水势: 涨/落/平 */
  trend: 'rising' | 'falling' | 'steady'
  /** 超警戒 (m)，负值表示低于警戒 */
  overWarning: number
  /** 告警等级 */
  alertLevel: AlertLevel
  /** 更新时间 */
  updateTime: string
}
