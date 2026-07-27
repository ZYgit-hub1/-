import { MOCK_DATA } from './data'
import type {
  PowerPlant,
  WeatherData,
  HydroData,
  Alarm,
  Warning,
  AlarmStatus,
  AlarmLevel,
  WarningLevel,
  WarningType
} from '@/types'

// ============================================================================
// 类型定义
// ============================================================================

/** 分页参数 */
export interface PaginationParams {
  page?: number
  pageSize?: number
}

/** 分页响应 */
export interface PageResponse<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

/** getPlants 入参 */
export interface GetPlantsParams extends PaginationParams {
  /** 名称或类型模糊搜索 */
  search?: string
  /** 按类型精确过滤 */
  type?: PowerPlant['type']
  /** 按状态过滤 */
  status?: PowerPlant['status']
}

/** getAlarms 入参 */
export interface GetAlarmsParams extends PaginationParams {
  status?: AlarmStatus
  level?: AlarmLevel
  plantId?: string
  keyword?: string
}

/** getWarnings 入参 */
export interface GetWarningsParams extends PaginationParams {
  level?: WarningLevel
  type?: WarningType
  plantId?: string
  status?: 'active' | 'expired' | 'cancelled'
}

/** 电厂详情：包含基础信息 + 历史数据 */
export interface PlantDetail extends PowerPlant {
  weatherHistory: WeatherData[]
  hydroHistory: HydroData[]
}

// ============================================================================
// 工具函数
// ============================================================================

/** 模拟网络延迟 200-500ms */
function delay<T>(data: T, error?: Error): Promise<T> {
  const ms = Math.floor(Math.random() * 301) + 200 // 200-500ms
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (error) reject(error)
      else resolve(data)
    }, ms)
  })
}

/** 分页工具 */
function paginate<T>(list: T[], page = 1, pageSize = 10): PageResponse<T> {
  const start = (page - 1) * pageSize
  return {
    list: list.slice(start, start + pageSize),
    total: list.length,
    page,
    pageSize
  }
}

/** 随机整数 */
function randInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

/** 从数组中随机取一个 */
function pick<T>(arr: readonly T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]
}

/** 生成 UUID */
function uuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// ============================================================================
// 内部数据：报警 & 预警（在模块加载时生成）
// ============================================================================

const ALARM_LEVELS: AlarmLevel[] = ['emergency', 'high', 'medium', 'low']
const ALARM_STATUSES: AlarmStatus[] = ['unconfirmed', 'confirmed', 'resolved']
const WARNING_LEVELS: WarningLevel[] = ['red', 'orange', 'yellow', 'blue', 'green']
const WARNING_TYPES: WarningType[] = ['weather', 'flood', 'fire', 'equipment', 'other']

const ALARM_CONTENT_TEMPLATES: Record<AlarmLevel, string[]> = {
  emergency: [
    '#1 机组汽轮机振动值严重超标（>12mm/s），触发紧急停机保护，请立即组织抢修。',
    '升压站 GIS 设备 SF6 气体压力急剧下降至报警值，存在绝缘击穿风险。',
    '主变压器内部故障气体含量异常，疑似放电故障。'
  ],
  high: [
    '#2 锅炉给水泵轴承温度持续高于 90°C，逼近跳闸值。',
    '主变压器负荷率超过 110%，持续时间达 30 分钟，存在过负荷跳闸风险。',
    '循环水泵房液位异常下降，疑似管道泄漏。',
    '氢冷系统氢气湿度超标，存在氢气泄漏风险。'
  ],
  medium: [
    '#3 磨煤机电流波动频繁，可能存在煤质变化或给煤机异常。',
    '脱硫系统出口 SO2 浓度超标（>200mg/Nm³），环保数据异常。',
    '输煤系统 #2 皮带撕裂检测传感器报警，皮带存在损伤。',
    '厂用电 10kV II 段母线接地告警，需排查接地故障。',
    '烟气在线监测 CEMS 分析仪采样泵故障，数据上传中断。'
  ],
  low: [
    '#1 冷却塔水位偏低，自动补水阀响应异常。',
    '消防泵房定期巡检发现 #3 消防泵启动困难。',
    '厂区周界红外对射报警器离线，监控存在盲区。',
    '门禁系统服务器与现场终端通信中断，影响人员出入管理。'
  ]
}

const WARNING_CONTENT_TEMPLATES: Record<WarningType, string[]> = {
  weather: [
    '台风"山竹"将于 24 小时内正面登陆广东沿海地区，预计登陆时中心风力达 16 级，请各电厂立即启动防台风 I 级应急响应。',
    '受台风外围环流影响，未来 48 小时内珠三角地区将出现大暴雨，过程累积雨量预计 200-300 毫米，需提前做好防汛准备。',
    '高温红色预警：粤北、粤西地区未来 3 天最高气温将达 40°C 以上，请各电厂加强设备降温。',
    '暴雨黄色预警：珠三角地区未来 6 小时内将出现强降雨，累积雨量 50-80 毫米。',
    '雷雨大风蓝色预警：粤西沿海地区未来 12 小时内将出现 8 级以上雷雨大风。',
    '寒潮橙色预警：粤北山区未来 24 小时气温将骤降 15°C 以上。'
  ],
  flood: [
    '北江流域出现超 50 年一遇特大洪水，清远、韶关地区电厂进水风险极高。',
    '东江流域水位持续上涨，预计未来 24 小时将接近警戒水位，请惠州、河源地区电厂加强巡检。',
    '西江水位快速上涨，预计未来 12 小时将超过警戒水位 1.5 米。'
  ],
  fire: [
    '粤东地区森林火险等级高企，汕头、潮州电厂周边山林存在火情隐患。',
    '厂区周边 10 公里范围内检测到野外火点，请加强巡逻并做好防火隔离。'
  ],
  equipment: [
    '#3 机组主变压器油温持续偏高，已超过预警阈值，请立即安排检修人员到现场排查。',
    '#2 锅炉承压部件检测发现疑似缺陷，请安排停机检修。',
    '升压站 #4 避雷器在线监测数据异常，建议尽快停电试验。'
  ],
  other: [
    '接到上级通知，今日 14:00-17:00 进行电网联合反事故演练，请各电厂保持通讯畅通。',
    '近期雷暴多发，请加强厂区防雷接地设施检查。',
    '环保部门专项检查将于本周开展，请各电厂做好迎检准备。'
  ]
}

const HANDLERS = ['张工', '李工', '王工', '赵工', '陈工', '刘工', '黄工', '周工', '吴工', '郑工', '孙工']

/** 生成初始报警列表（约 50 条） */
function generateAlarms(): Alarm[] {
  const alarms: Alarm[] = []
  const count = 50
  for (let i = 0; i < count; i++) {
    const level = pick(ALARM_LEVELS)
    const status = pick(ALARM_STATUSES)
    const plant = pick(MOCK_DATA.plants)
    const triggerOffset = randInt(1, 168) // 1-168 小时前
    const triggerTime = new Date(Date.now() - triggerOffset * 3600 * 1000).toISOString()

    let confirmTime: string | undefined
    let resolveTime: string | undefined
    if (status === 'confirmed' || status === 'resolved') {
      confirmTime = new Date(
        Date.now() - (triggerOffset - randInt(1, 4)) * 3600 * 1000
      ).toISOString()
    }
    if (status === 'resolved') {
      resolveTime = new Date(
        Date.now() - (triggerOffset - randInt(4, 24)) * 3600 * 1000
      ).toISOString()
    }

    alarms.push({
      id: uuid(),
      level,
      status,
      content: pick(ALARM_CONTENT_TEMPLATES[level]),
      plantId: plant.id,
      plantName: plant.name,
      triggerTime,
      confirmTime,
      resolveTime,
      handler: status !== 'unconfirmed' ? pick(HANDLERS) : undefined,
      remark:
        status === 'resolved'
          ? '已处理完毕，恢复正常运行'
          : status === 'confirmed'
          ? '已安排人员现场处理'
          : undefined
    })
  }
  // 按触发时间倒序
  return alarms.sort((a, b) => b.triggerTime.localeCompare(a.triggerTime))
}

/** 生成初始预警列表（约 30 条） */
function generateWarnings(): Warning[] {
  const warnings: Warning[] = []
  const count = 30
  for (let i = 0; i < count; i++) {
    const level = pick(WARNING_LEVELS)
    const type = pick(WARNING_TYPES)
    const plant = pick(MOCK_DATA.plants)
    const startOffset = randInt(1, 72) // 1-72 小时前
    const startTime = new Date(Date.now() - startOffset * 3600 * 1000).toISOString()

    // 状态分布：60% active / 30% expired / 10% cancelled
    const statusRoll = Math.random()
    const status: Warning['status'] =
      statusRoll < 0.6 ? 'active' : statusRoll < 0.9 ? 'expired' : 'cancelled'

    const endTime =
      status !== 'active'
        ? new Date(Date.now() - (startOffset - randInt(6, 24)) * 3600 * 1000).toISOString()
        : undefined

    warnings.push({
      id: uuid(),
      level,
      type,
      content: pick(WARNING_CONTENT_TEMPLATES[type]),
      plantId: plant.id,
      plantName: plant.name,
      startTime,
      endTime,
      status
    })
  }
  // 按开始时间倒序
  return warnings.sort((a, b) => b.startTime.localeCompare(a.startTime))
}

/** 内存中可变的报警 / 预警列表（模拟后端"数据库"） */
const alarmsStore: Alarm[] = generateAlarms()
const warningsStore: Warning[] = generateWarnings()

// ============================================================================
// MockAPI 类
// ============================================================================

export class MockAPI {
  // --------------------------------------------------------------------------
  // 电厂相关
  // --------------------------------------------------------------------------

  /**
   * 获取电厂列表
   * 支持 search（名称/类型模糊）、type、status 过滤
   */
  async getPlants(params: GetPlantsParams = {}): Promise<PageResponse<PowerPlant>> {
    const { search, type, status, page = 1, pageSize = 10 } = params

    let list = [...MOCK_DATA.plants]

    if (search) {
      const kw = search.toLowerCase().trim()
      list = list.filter(
        p =>
          p.name.toLowerCase().includes(kw) ||
          p.type.toLowerCase().includes(kw) ||
          (p.address && p.address.toLowerCase().includes(kw))
      )
    }

    if (type) {
      list = list.filter(p => p.type === type)
    }

    if (status) {
      list = list.filter(p => p.status === status)
    }

    return delay(paginate(list, page, pageSize))
  }

  /**
   * 获取电厂详情
   * 包含电厂基础信息 + 历史气象 + 历史水文
   */
  async getPlantDetail(plantId: string): Promise<PlantDetail> {
    const plant = MOCK_DATA.plants.find(p => p.id === plantId)
    if (!plant) {
      return delay({} as PlantDetail, new Error(`Plant ${plantId} not found`))
    }

    const detail: PlantDetail = {
      ...plant,
      weatherHistory: MOCK_DATA.weatherHistory[plantId] || [],
      hydroHistory: MOCK_DATA.hydroHistory[plantId] || []
    }

    return delay(detail)
  }

  // --------------------------------------------------------------------------
  // 预警相关
  // --------------------------------------------------------------------------

  /**
   * 获取预警列表
   * 支持 level、type、plantId、status 过滤
   */
  async getWarnings(params: GetWarningsParams = {}): Promise<PageResponse<Warning>> {
    const { level, type, plantId, status, page = 1, pageSize = 10 } = params

    let list = [...warningsStore]

    if (level) list = list.filter(w => w.level === level)
    if (type) list = list.filter(w => w.type === type)
    if (plantId) list = list.filter(w => w.plantId === plantId)
    if (status) list = list.filter(w => w.status === status)

    return delay(paginate(list, page, pageSize))
  }

  // --------------------------------------------------------------------------
  // 报警相关
  // --------------------------------------------------------------------------

  /**
   * 获取报警列表
   * 支持 status、level、plantId、keyword 过滤
   */
  async getAlarms(params: GetAlarmsParams = {}): Promise<PageResponse<Alarm>> {
    const { status, level, plantId, keyword, page = 1, pageSize = 10 } = params

    let list = [...alarmsStore]

    if (status) list = list.filter(a => a.status === status)
    if (level) list = list.filter(a => a.level === level)
    if (plantId) list = list.filter(a => a.plantId === plantId)
    if (keyword) {
      const kw = keyword.toLowerCase().trim()
      list = list.filter(
        a => a.content.toLowerCase().includes(kw) || a.plantName.toLowerCase().includes(kw)
      )
    }

    return delay(paginate(list, page, pageSize))
  }

  /**
   * 更新报警状态
   * 模拟后端 PATCH，成功后返回更新后的对象
   */
  async updateAlarmStatus(alarmId: string, status: AlarmStatus): Promise<Alarm> {
    const alarm = alarmsStore.find(a => a.id === alarmId)
    if (!alarm) {
      return delay({} as Alarm, new Error(`Alarm ${alarmId} not found`))
    }

    alarm.status = status
    const now = new Date().toISOString()

    if (status === 'confirmed' && !alarm.confirmTime) {
      alarm.confirmTime = now
      alarm.handler = alarm.handler || pick(HANDLERS)
    }
    if (status === 'resolved' && !alarm.resolveTime) {
      alarm.resolveTime = now
      alarm.handler = alarm.handler || pick(HANDLERS)
      if (!alarm.confirmTime) alarm.confirmTime = now
    }

    return delay({ ...alarm })
  }

  // --------------------------------------------------------------------------
  // 辅助方法（不属于核心需求，但便于调试）
  // --------------------------------------------------------------------------

  /** 重置内存数据（用于测试） */
  reset(): void {
    alarmsStore.length = 0
    warningsStore.length = 0
    alarmsStore.push(...generateAlarms())
    warningsStore.push(...generateWarnings())
  }

  /** 当前报警总数 */
  getAlarmCount(): number {
    return alarmsStore.length
  }

  /** 当前预警总数 */
  getWarningCount(): number {
    return warningsStore.length
  }
}

// ============================================================================
// 单例导出
// ============================================================================

export const mockApi = new MockAPI()

export default mockApi
