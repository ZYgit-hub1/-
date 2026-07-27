import type {
  PowerPlant,
  WeatherData,
  HydroData,
  Alarm,
  Warning,
  AlertLevel,
  HydroStation,
  HydroStationReading
} from '@/types'

// ============================================================================
// 工具函数（纯 JS / TS，不引入外部库）
// ============================================================================

/** 随机整数（含 min, max） */
function randInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

/** 随机浮点数 */
function randFloat(min: number, max: number, decimals = 2): number {
  return Number((Math.random() * (max - min) + min).toFixed(decimals))
}

/** 从数组中随机取一个 */
function pick<T>(arr: readonly T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]
}

/** 生成过去 N 小时内均匀分布的时间戳（ISO 字符串） */
function randomTimestampInPast24h(): string {
  const now = Date.now()
  // 24 小时 = 86_400_000 ms,均匀分布在 [now - 24h, now]
  const offsetMs = Math.floor(Math.random() * 86_400_000)
  return new Date(now - offsetMs).toISOString()
}

/** 生成 UUID v4（兼容老环境） */
function uuid(): string {
  // RFC 4122 v4
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// ============================================================================
// 广东省地理与电厂配置
// ============================================================================

/**
 * 广东主要城市经纬度范围（lng, lat）
 * 经度范围: 110.0° - 118.0°
 * 纬度范围: 20.0° - 25.5°
 */
interface CityGeo {
  name: string
  /** 沿海城市权重更高 */
  coastal: boolean
  lngRange: [number, number]
  latRange: [number, number]
}

const CITIES: CityGeo[] = [
  // 珠三角（沿海）
  { name: '广州', coastal: false, lngRange: [112.95, 113.55], latRange: [22.95, 23.50] },
  { name: '深圳', coastal: true,  lngRange: [113.85, 114.55], latRange: [22.45, 22.85] },
  { name: '东莞', coastal: false, lngRange: [113.60, 114.20], latRange: [22.70, 23.10] },
  { name: '佛山', coastal: false, lngRange: [112.80, 113.30], latRange: [22.80, 23.30] },
  { name: '珠海', coastal: true,  lngRange: [113.20, 113.65], latRange: [21.80, 22.45] },
  { name: '中山', coastal: true,  lngRange: [113.20, 113.55], latRange: [22.30, 22.70] },
  { name: '江门', coastal: true,  lngRange: [112.10, 113.10], latRange: [21.80, 22.80] },
  { name: '惠州', coastal: true,  lngRange: [114.20, 114.85], latRange: [22.80, 23.50] },
  { name: '肇庆', coastal: false, lngRange: [112.30, 112.85], latRange: [22.95, 23.50] },
  // 粤东（沿海）
  { name: '汕头', coastal: true,  lngRange: [116.55, 116.95], latRange: [23.20, 23.55] },
  { name: '潮州', coastal: true,  lngRange: [116.45, 116.85], latRange: [23.55, 23.95] },
  { name: '揭阳', coastal: false, lngRange: [115.85, 116.40], latRange: [23.30, 23.80] },
  { name: '汕尾', coastal: true,  lngRange: [115.30, 115.85], latRange: [22.70, 23.10] },
  // 粤西（沿海）
  { name: '湛江', coastal: true,  lngRange: [110.10, 110.70], latRange: [20.80, 21.50] },
  { name: '茂名', coastal: true,  lngRange: [110.80, 111.30], latRange: [21.40, 22.00] },
  { name: '阳江', coastal: true,  lngRange: [111.55, 112.10], latRange: [21.70, 22.20] },
  // 粤北（内陆）
  { name: '韶关', coastal: false, lngRange: [113.50, 114.10], latRange: [24.70, 25.20] },
  { name: '清远', coastal: false, lngRange: [112.90, 113.50], latRange: [23.50, 24.30] },
  { name: '河源', coastal: false, lngRange: [114.60, 115.30], latRange: [23.70, 24.40] },
  { name: '梅州', coastal: false, lngRange: [115.80, 116.50], latRange: [24.10, 24.80] },
  { name: '云浮', coastal: false, lngRange: [111.85, 112.40], latRange: [22.50, 23.10] }
]

/** 电厂类型命名 */
const PLANT_TYPE_NAMES: Record<PowerPlant['type'], string[]> = {
  coal: ['燃煤电厂', '热电厂', '煤电基地'],
  gas: ['燃气电厂', '天然气电厂', 'LNG电厂'],
  solar: ['光伏电站', '太阳能电站', '光热电站'],
  wind: ['风电场', '海上风电场', '陆上风电场'],
  storage: ['抽水蓄能电站', '储能电站', '电池储能站']
}

/** 根据类型估算典型装机容量（MW） */
function getTypicalCapacity(type: PowerPlant['type']): number {
  switch (type) {
    case 'coal':
      return randInt(600, 2000)
    case 'gas':
      return randInt(300, 1200)
    case 'solar':
      return randInt(50, 500)
    case 'wind':
      return randInt(100, 800)
    case 'storage':
      return randInt(50, 300)
  }
}

/**
 * 选取城市：沿海城市权重更高（约 60%），符合广东地图形状
 */
function pickWeightedCity(): CityGeo {
  const coastalCities = CITIES.filter(c => c.coastal)
  const inlandCities = CITIES.filter(c => !c.coastal)
  return Math.random() < 0.6 ? pick(coastalCities) : pick(inlandCities)
}

/** 生成单个电厂对象 */
function generatePlant(index: number): PowerPlant {
  const city = pickWeightedCity()
  const type = pick<PowerPlant['type']>(['coal', 'gas', 'solar', 'wind', 'storage'])
  // 默认 normal,随机 10% 为 warning
  const status: PowerPlant['status'] = Math.random() < 0.1 ? 'warning' : 'normal'

  const typePrefix = pick(PLANT_TYPE_NAMES[type])
  const num = String(index + 1).padStart(2, '0')

  return {
    id: uuid(),
    name: `${city.name}${typePrefix}-${num}`,
    type,
    location: {
      lng: randFloat(city.lngRange[0], city.lngRange[1], 6),
      lat: randFloat(city.latRange[0], city.latRange[1], 6)
    },
    status,
    capacity: getTypicalCapacity(type),
    address: `广东省${city.name}市${pick(['开发区', '高新区', '工业园区', '港区', '经济区'])}${randInt(1, 200)}号`
  }
}

// ============================================================================
// 1. 电厂数据：中国华电集团广东省电厂（真实布局）
//    数据来源：中国华电集团有限公司广东公司下属企业
// ============================================================================

/** 华电集团在广东的电厂真实信息 */
interface HuadianPlantInfo {
  name: string
  type: PowerPlant['type']
  lng: number
  lat: number
  capacity: number
  address: string
  status: PowerPlant['status']
}

const HUADIAN_GUANGDONG_PLANTS: HuadianPlantInfo[] = [
  {
    name: '汕头华电发电有限公司',
    type: 'coal',
    lng: 116.6821,
    lat: 23.3535,
    capacity: 1200,
    address: '广东省汕头市濠江区广澳港',
    status: 'normal'
  },
  {
    name: '华电福新广州能源有限公司',
    type: 'gas',
    lng: 113.4056,
    lat: 23.0456,
    capacity: 1200,
    address: '广东省广州市南沙区黄阁镇',
    status: 'normal'
  },
  {
    name: '广东华电清远能源有限公司',
    type: 'gas',
    lng: 113.0624,
    lat: 23.6821,
    capacity: 600,
    address: '广东省清远市清城区石角镇',
    status: 'normal'
  },
  {
    name: '广东华电惠州能源有限公司',
    type: 'gas',
    lng: 114.4123,
    lat: 23.1115,
    capacity: 900,
    address: '广东省惠州市大亚湾石化区',
    status: 'normal'
  },
  {
    name: '广东华电坪石发电有限公司',
    type: 'coal',
    lng: 113.0521,
    lat: 25.2856,
    capacity: 700,
    address: '广东省韶关乐昌市坪石镇',
    status: 'normal'
  },
  {
    name: '广东华电韶关热电有限公司',
    type: 'coal',
    lng: 113.6024,
    lat: 24.8123,
    capacity: 700,
    address: '广东省韶关市浈江区乐园镇',
    status: 'normal'
  },
  {
    name: '广东华电福新阳江海上风电有限公司',
    type: 'wind',
    lng: 111.9821,
    lat: 21.8635,
    capacity: 500,
    address: '广东省阳江市阳西县溪头镇海域',
    status: 'normal'
  },
  {
    name: '华电新能源集团股份有限公司广东分公司',
    type: 'solar',
    lng: 113.2644,
    lat: 23.1291,
    capacity: 400,
    address: '广东省广州市萝岗区开发区科汇四街3号',
    status: 'normal'
  },
  {
    name: '广东华电深圳能源有限公司',
    type: 'gas',
    lng: 114.0579,
    lat: 22.5431,
    capacity: 800,
    address: '广东省深圳市宝安区福永街道',
    status: 'normal'
  }
]

function buildPlants(): PowerPlant[] {
  return HUADIAN_GUANGDONG_PLANTS.map((info, index) => ({
    id: `huadian-gd-${String(index + 1).padStart(2, '0')}`,
    name: info.name,
    type: info.type,
    location: {
      lng: info.lng,
      lat: info.lat
    },
    status: info.status,
    capacity: info.capacity,
    address: info.address
  }))
}

const plants: PowerPlant[] = buildPlants()

// ============================================================================
// 2. 气象历史数据：每个电厂过去 24 小时
// ============================================================================

/** 按日变化曲线生成基础温度（凌晨低、下午高） */
function baseTempByHour(hour: number): number {
  // 14 点最高，凌晨 5 点最低
  const phase = ((hour - 14) / 24) * Math.PI * 2
  return 27 + 6 * Math.cos(phase)
}

function generateWeatherHistory(plantId: string): WeatherData[] {
  const list: WeatherData[] = []
  const now = new Date()

  // 为该电厂确定一个基础值，保证 24h 趋势连续
  const baseTemp = randFloat(20, 35)
  const baseHumidity = randInt(50, 90)
  const baseWind = randFloat(2, 10)
  const basePressure = randFloat(1005, 1020)

  for (let i = 23; i >= 0; i--) {
    const d = new Date(now)
    d.setHours(d.getHours() - i, 0, 0, 0)
    const hour = d.getHours()

    // 温度：在日变化曲线基础上加随机扰动
    const diurnal = baseTempByHour(hour)
    const temp = Number((diurnal + randFloat(-1.5, 1.5)).toFixed(1))

    // 湿度：与温度负相关
    const humidity = Math.max(
      30,
      Math.min(100, Math.round(baseHumidity + (baseTemp - temp) * 1.5 + randInt(-5, 5)))
    )

    // 风速：2-10 m/s 波动
    const windSpeed = Number(
      Math.max(0, baseWind + randFloat(-2, 2)).toFixed(1)
    )

    // 风向：0-359 度
    const windDirection = randInt(0, 359)

    // 降雨量：30% 概率有降雨
    const rainfall = Math.random() < 0.3 ? randFloat(0.1, 25, 1) : 0

    // 气压
    const pressure = Number(
      Math.max(990, Math.min(1030, basePressure + randFloat(-3, 3))).toFixed(1)
    )

    list.push({
      temp,
      humidity,
      windSpeed,
      windDirection,
      rainfall,
      pressure,
      updateTime: d.toISOString()
    })
  }

  return list
}

// ============================================================================
// 3. 水文历史数据：每个电厂过去 24 小时
// ============================================================================

const WATER_LEVEL_WARNING_LINE = 13.0 // 警戒线（米）

function judgeAlertLevel(waterLevel: number): AlertLevel {
  if (waterLevel >= 14.5) return 'flood'
  if (waterLevel >= WATER_LEVEL_WARNING_LINE) return 'warning'
  if (waterLevel >= 11.5) return 'watch'
  return 'normal'
}

function generateHydroHistory(plantId: string): HydroData[] {
  const list: HydroData[] = []
  const now = new Date()

  // 基准水位（10-15m）、基准流量（500-1200 m³/s）
  const baseLevel = randFloat(10.0, 15.0, 2)
  const baseFlow = randInt(500, 1200)

  for (let i = 23; i >= 0; i--) {
    const d = new Date(now)
    d.setHours(d.getHours() - i, 0, 0, 0)

    // 水位围绕基准小幅波动
    const waterLevel = Number(
      Math.max(9.0, Math.min(15.5, baseLevel + randFloat(-0.4, 0.4))).toFixed(2)
    )
    // 流量围绕基准波动
    const flowRate = Math.max(
      200,
      Math.round(baseFlow + randInt(-150, 150))
    )

    list.push({
      waterLevel,
      flowRate,
      alertLevel: judgeAlertLevel(waterLevel),
      updateTime: d.toISOString()
    })
  }

  return list
}

// ============================================================================
// 4. 广东省水文站数据
// ============================================================================

/**
 * 广东省主要水文站（基于真实水文站点近似经纬度）
 * 覆盖五大流域: 西江、北江、东江、韩江、鉴江 + 珠江三角洲
 */
const HYDRO_STATIONS_RAW: Array<Omit<HydroStation, 'id' | 'current'>> = [
  // 西江干流
  { name: '高要水文站', river: '西江', city: '肇庆', location: { lng: 112.27, lat: 23.05 }, warningLevel: 9.5, guaranteeLevel: 13.0, historicalMax: 13.62 },
  { name: '马口水文站', river: '西江', city: '佛山', location: { lng: 112.81, lat: 23.17 }, warningLevel: 7.5, guaranteeLevel: 9.5, historicalMax: 10.99 },
  // 北江干流
  { name: '石角水文站', river: '北江', city: '清远', location: { lng: 112.96, lat: 23.55 }, warningLevel: 11.0, guaranteeLevel: 14.5, historicalMax: 15.36 },
  { name: '清远水文站', river: '北江', city: '清远', location: { lng: 113.06, lat: 23.68 }, warningLevel: 12.0, guaranteeLevel: 15.0, historicalMax: 15.88 },
  { name: '三水水文站', river: '北江', city: '佛山', location: { lng: 112.87, lat: 23.18 }, warningLevel: 8.5, guaranteeLevel: 10.5, historicalMax: 11.96 },
  // 东江干流
  { name: '河源水文站', river: '东江', city: '河源', location: { lng: 114.70, lat: 23.74 }, warningLevel: 39.0, guaranteeLevel: 42.0, historicalMax: 43.31 },
  { name: '岭下水文站', river: '东江', city: '惠州', location: { lng: 114.55, lat: 23.25 }, warningLevel: 15.5, guaranteeLevel: 18.0, historicalMax: 19.85 },
  { name: '博罗水文站', river: '东江', city: '惠州', location: { lng: 114.29, lat: 23.17 }, warningLevel: 11.2, guaranteeLevel: 13.0, historicalMax: 14.46 },
  // 韩江水系
  { name: '溪口水文站', river: '韩江', city: '梅州', location: { lng: 116.12, lat: 24.29 }, warningLevel: 14.5, guaranteeLevel: 16.5, historicalMax: 17.27 },
  { name: '潮安水文站', river: '韩江', city: '潮州', location: { lng: 116.68, lat: 23.46 }, warningLevel: 13.5, guaranteeLevel: 15.5, historicalMax: 16.95 },
  // 鉴江水系
  { name: '化州水文站', river: '鉴江', city: '茂名', location: { lng: 110.60, lat: 21.66 }, warningLevel: 13.5, guaranteeLevel: 15.8, historicalMax: 17.36 },
  { name: '缸瓦窑水文站', river: '鉴江', city: '湛江', location: { lng: 110.36, lat: 21.27 }, warningLevel: 7.0, guaranteeLevel: 9.0, historicalMax: 9.87 },
  // 珠江三角洲
  { name: '广州水文站', river: '珠江', city: '广州', location: { lng: 113.27, lat: 23.13 }, warningLevel: 6.5, guaranteeLevel: 8.0, historicalMax: 8.96 },
  { name: '中山水文站', river: '珠江', city: '中山', location: { lng: 113.39, lat: 22.52 }, warningLevel: 4.5, guaranteeLevel: 6.0, historicalMax: 6.78 },
  { name: '江门水文站', river: '潭江', city: '江门', location: { lng: 113.08, lat: 22.58 }, warningLevel: 5.5, guaranteeLevel: 7.0, historicalMax: 7.82 },
  // 粤东沿海
  { name: '汕尾水文站', river: '螺河', city: '汕尾', location: { lng: 115.37, lat: 22.79 }, warningLevel: 6.0, guaranteeLevel: 8.0, historicalMax: 8.64 },
  { name: '汕头水文站', river: '榕江', city: '汕头', location: { lng: 116.68, lat: 23.35 }, warningLevel: 12.0, guaranteeLevel: 14.0, historicalMax: 14.78 },
  // 粤西沿海
  { name: '阳江水文站', river: '漠阳江', city: '阳江', location: { lng: 111.98, lat: 21.86 }, warningLevel: 6.5, guaranteeLevel: 8.5, historicalMax: 9.12 },
  { name: '茂名水文站', river: '小东江', city: '茂名', location: { lng: 110.93, lat: 21.66 }, warningLevel: 11.0, guaranteeLevel: 13.0, historicalMax: 13.74 },
]

/** 为单个水文站生成实时读数 */
function generateStationReading(station: Omit<HydroStation, 'id' | 'current'>): HydroStationReading {
  // 水位在警戒水位附近波动，约 30% 超警
  const overWarnRatio = Math.random()
  let waterLevel: number
  if (overWarnRatio < 0.3) {
    // 超警: 警戒水位 ~ 保证水位
    waterLevel = randFloat(station.warningLevel, station.guaranteeLevel, 2)
  } else if (overWarnRatio < 0.4) {
    // 超保证水位
    waterLevel = randFloat(station.guaranteeLevel, station.guaranteeLevel + 1.5, 2)
  } else {
    // 正常: 警戒水位 -2 ~ 警戒水位
    waterLevel = randFloat(Math.max(0, station.warningLevel - 2), station.warningLevel, 2)
  }

  const overWarning = Number((waterLevel - station.warningLevel).toFixed(2))

  // 告警等级
  let alertLevel: AlertLevel = 'normal'
  if (waterLevel >= station.guaranteeLevel) alertLevel = 'flood'
  else if (waterLevel >= station.warningLevel) alertLevel = 'warning'
  else if (waterLevel >= station.warningLevel - 1) alertLevel = 'watch'

  // 流量随水位变化
  const flowBase = randInt(200, 2500)
  const flowRate = Math.round(flowBase * (0.6 + (waterLevel / station.warningLevel) * 0.8))

  // 水势
  const trendRoll = Math.random()
  const trend: HydroStationReading['trend'] =
    trendRoll < 0.4 ? 'rising' : trendRoll < 0.75 ? 'falling' : 'steady'

  return {
    waterLevel,
    flowRate,
    trend,
    overWarning,
    alertLevel,
    updateTime: new Date().toISOString()
  }
}

/** 构建水文站列表（带实时读数） */
function buildHydroStations(): HydroStation[] {
  return HYDRO_STATIONS_RAW.map((s, i) => ({
    ...s,
    id: `hydro-${String(i + 1).padStart(2, '0')}`,
    current: generateStationReading(s)
  }))
}

const hydroStations: HydroStation[] = buildHydroStations()



const weatherHistory: Record<string, WeatherData[]> = {}
const hydroHistory: Record<string, HydroData[]> = {}

plants.forEach(plant => {
  weatherHistory[plant.id] = generateWeatherHistory(plant.id)
  hydroHistory[plant.id] = generateHydroHistory(plant.id)
})

/**
 * Mock 数据集合
 * - plants: 50 个电厂对象
 * - weatherHistory: 每个电厂最近 24 小时的气象数据，键为 plantId
 * - hydroHistory: 每个电厂最近 24 小时的水文数据，键为 plantId
 * - hydroStations: 广东省主要水文站列表（含实时水位读数）
 */
export const MOCK_DATA = {
  plants,
  weatherHistory,
  hydroHistory,
  hydroStations
}

export default MOCK_DATA

// ============================================================================
// 兼容垫片: mockDataService
// 说明:
//   部分旧 view（HomePage/Statistics/AlarmCenter/AlarmDetail/WarningList/PlantDetail）
//   仍通过 `import { mockDataService } from '@/mock/data'` 直接访问数据。
//   该对象用于在 store/mockApi 重构期间保持这些 view 的运行时可用，
//   应当视为过渡产物，后续将逐步替换为 store 接入。
// ============================================================================

import type { AlarmLevel, AlarmStatus, DashboardStats, HydroStation } from '@/types'

const ALARM_LEVELS: AlarmLevel[] = ['emergency', 'high', 'medium', 'low']
const ALARM_STATUSES: AlarmStatus[] = ['unconfirmed', 'confirmed', 'resolved']

function pickRandom<T>(arr: readonly T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]
}

/** 生成与 view 兼容的本地报警列表（独立于 mockApi 内部 store） */
function generateLocalAlarms(): Alarm[] {
  const list: Alarm[] = []
  for (let i = 0; i < 50; i++) {
    const plant = pickRandom(MOCK_DATA.plants)
    const level = pickRandom(ALARM_LEVELS)
    const status = pickRandom(ALARM_STATUSES)
    const triggerOffset = Math.floor(Math.random() * 168) + 1
    const triggerTime = new Date(Date.now() - triggerOffset * 3600 * 1000).toISOString()

    let confirmTime: string | undefined
    let resolveTime: string | undefined
    let handler: string | undefined
    let remark: string | undefined
    if (status !== 'unconfirmed') {
      confirmTime = new Date(Date.now() - (triggerOffset - 2) * 3600 * 1000).toISOString()
      handler = '现场值班'
    }
    if (status === 'resolved') {
      resolveTime = new Date(Date.now() - (triggerOffset - 12) * 3600 * 1000).toISOString()
      remark = '已处理完毕，恢复正常运行'
    } else if (status === 'confirmed') {
      remark = '已安排人员现场处理'
    }

    list.push({
      id: uuid(),
      level,
      status,
      content: `系统报警: ${plant.name} 设备参数异常，请及时处理。`,
      plantId: plant.id,
      plantName: plant.name,
      triggerTime,
      confirmTime,
      resolveTime,
      handler,
      remark
    })
  }
  return list.sort((a, b) => b.triggerTime.localeCompare(a.triggerTime))
}

/** 生成与 view 兼容的本地预警列表 */
function generateLocalWarnings(): Warning[] {
  const list: Warning[] = []
  const levels: Warning['level'][] = ['green', 'blue', 'yellow', 'orange', 'red']
  const types: Warning['type'][] = ['weather', 'flood', 'fire', 'equipment', 'other']
  for (let i = 0; i < 30; i++) {
    const plant = pickRandom(MOCK_DATA.plants)
    const level = pickRandom(levels)
    const type = pickRandom(types)
    const startOffset = Math.floor(Math.random() * 72) + 1
    const startTime = new Date(Date.now() - startOffset * 3600 * 1000).toISOString()
    const statusRoll = Math.random()
    const status: Warning['status'] =
      statusRoll < 0.6 ? 'active' : statusRoll < 0.9 ? 'expired' : 'cancelled'
    list.push({
      id: uuid(),
      level,
      type,
      content: `气象预警: ${plant.name} 周边监测到异常情况，请关注。`,
      plantId: plant.id,
      plantName: plant.name,
      startTime,
      endTime: status !== 'active'
        ? new Date(Date.now() - (startOffset - 6) * 3600 * 1000).toISOString()
        : undefined,
      status
    })
  }
  return list.sort((a, b) => b.startTime.localeCompare(a.startTime))
}

const _localAlarms: Alarm[] = generateLocalAlarms()
const _localWarnings: Warning[] = generateLocalWarnings()

/**
 * 兼容旧 view 的同步数据访问层。
 * 数据为模块加载时一次性生成的副本；与 store / mockApi 中的可变 store 不共享。
 * 适合 view 的只读渲染场景。
 */
export const mockDataService = {
  /** 获取所有电厂（返回浅拷贝） */
  getPlants(): PowerPlant[] {
    return [...MOCK_DATA.plants]
  },

  /** 按 ID 查找电厂 */
  getPlantById(id: string): PowerPlant | undefined {
    return MOCK_DATA.plants.find(p => p.id === id)
  },

  /** 获取电厂最近一条气象数据 */
  getWeatherData(id: string): WeatherData | null {
    const list = MOCK_DATA.weatherHistory[id]
    return list && list.length ? list[list.length - 1] : null
  },

  /** 获取电厂最近一条水文数据 */
  getHydroData(id: string): HydroData | null {
    const list = MOCK_DATA.hydroHistory[id]
    return list && list.length ? list[list.length - 1] : null
  },

  /** 获取所有水文站（含实时水位读数） */
  getHydroStations(): HydroStation[] {
    return [...MOCK_DATA.hydroStations]
  },

  /** 按 ID 查找水文站 */
  getHydroStationById(id: string): HydroStation | undefined {
    return MOCK_DATA.hydroStations.find(s => s.id === id)
  },

  /** 获取所有报警（返回浅拷贝） */
  getAlarms(): Alarm[] {
    return [..._localAlarms]
  },

  /** 获取所有预警（返回浅拷贝） */
  getWarnings(): Warning[] {
    return [..._localWarnings]
  },

  /** 聚合 Dashboard 统计 */
  getStats(): DashboardStats {
    const totalPlants = MOCK_DATA.plants.length
    const normalPlants = MOCK_DATA.plants.filter(p => p.status === 'normal').length
    const warningPlants = MOCK_DATA.plants.filter(p => p.status === 'warning').length
    const dangerPlants = MOCK_DATA.plants.filter(p => p.status === 'danger').length
    const totalAlarms = _localAlarms.length
    const unreadAlarms = _localAlarms.filter(a => a.status === 'unconfirmed').length
    const activeWarnings = _localWarnings.filter(w => w.status === 'active').length
    return {
      totalPlants,
      normalPlants,
      warningPlants,
      dangerPlants,
      totalAlarms,
      unreadAlarms,
      activeWarnings
    }
  }
}
