/**
 * MSW v2 Mock Handlers -- 广东省电厂监控平台
 *
 * 模拟 Gateway (localhost:8080) 下的所有微服务 REST 端点。
 * 响应格式: R<T>  =>  { code: 200, msg: "操作成功", data: ... }
 * 分页格式: PageResult<T>  =>  { list: [...], total: N, page: 1, size: 10 }
 */
import { http, HttpResponse, delay } from 'msw';

// ---------------------------------------------------------------------------
//  Mock Token
// ---------------------------------------------------------------------------
const MOCK_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4iLCJpYXQiOjE3ODUxMzYwMDAsImV4cCI6MTc4NTE0MjgwMH0.mock_signature';

/** 非 Auth 接口要求 Authorization Bearer Token */
function requireAuth(request: Request): boolean {
  const header = request.headers.get('Authorization');
  return header != null && header.startsWith('Bearer ');
}

/** 成功响应工厂 */
function ok<T>(data: T) {
  return HttpResponse.json({
    code: 200,
    msg: '操作成功',
    data,
  });
}

function okWithoutData() {
  return HttpResponse.json({
    code: 200,
    msg: '操作成功',
    data: null,
  });
}

function fail(code: number, msg: string) {
  return HttpResponse.json({ code, msg, data: null }, { status: code === 401 ? 401 : 400 });
}

// ---------------------------------------------------------------------------
//  Mock Data -- Plants (3)
// ---------------------------------------------------------------------------
const mockPlants = [
  {
    id: 1,
    name: '华电广州燃煤电厂',
    type: 'coal',
    lng: 113.264385,
    lat: 23.129112,
    capacity: 600.00,
    status: 'normal',
    address: '广东省广州市黄埔区开发大道388号',
    createTime: '2024-01-15 08:30:00',
    updateTime: '2026-07-20 14:22:00',
  },
  {
    id: 2,
    name: '华电深圳燃气电厂',
    type: 'gas',
    lng: 114.057868,
    lat: 22.543099,
    capacity: 450.00,
    status: 'warning',
    address: '广东省深圳市宝安区福永街道凤凰山工业园',
    createTime: '2024-03-22 10:00:00',
    updateTime: '2026-07-25 09:15:00',
  },
  {
    id: 3,
    name: '华电阳江风电场',
    type: 'wind',
    lng: 111.982288,
    lat: 21.858099,
    capacity: 200.00,
    status: 'normal',
    address: '广东省阳江市江城区白沙街道海陵湾',
    createTime: '2024-06-01 09:00:00',
    updateTime: '2026-07-24 16:45:00',
  },
];

// ---------------------------------------------------------------------------
//  Mock Data -- Hydro Stations (2)
// ---------------------------------------------------------------------------
const mockHydroStations = [
  {
    id: 1,
    name: '北江石角水文站',
    river: '北江',
    city: '清远',
    lng: 113.0936,
    lat: 23.5588,
    warningLevel: 11.50,
    guaranteeLevel: 12.80,
    historicalMax: 13.42,
    status: 'normal',
  },
  {
    id: 2,
    name: '东江博罗水文站',
    river: '东江',
    city: '惠州',
    lng: 114.2895,
    lat: 23.1720,
    warningLevel: 10.80,
    guaranteeLevel: 11.60,
    historicalMax: 12.15,
    status: 'warning',
  },
];

// ---------------------------------------------------------------------------
//  Mock Data -- Alarms (5)
// ---------------------------------------------------------------------------
const now = '2026-07-27 14:30:00';
const mockAlarms = [
  {
    id: 1,
    level: 'emergency',
    status: 'unconfirmed',
    content: '1号机组主变压器温度超限（85℃）',
    plantId: 1,
    triggerTime: '2026-07-27 13:05:00',
    confirmTime: null,
    resolveTime: null,
    handler: null,
    remark: null,
  },
  {
    id: 2,
    level: 'high',
    status: 'confirmed',
    content: '循环水泵出口压力异常下降（0.32MPa，阈值0.40MPa）',
    plantId: 2,
    triggerTime: '2026-07-27 11:20:00',
    confirmTime: '2026-07-27 11:35:00',
    resolveTime: null,
    handler: '张伟',
    remark: null,
  },
  {
    id: 3,
    level: 'medium',
    status: 'resolved',
    content: '2号锅炉排烟温度偏高（148℃，阈值145℃）',
    plantId: 1,
    triggerTime: '2026-07-27 09:10:00',
    confirmTime: '2026-07-27 09:25:00',
    resolveTime: '2026-07-27 10:00:00',
    handler: '李明',
    remark: '已调整风煤比，温度回落至正常范围',
  },
  {
    id: 4,
    level: 'low',
    status: 'unconfirmed',
    content: '厂用电率偏高（7.2%，阈值7.0%）',
    plantId: 3,
    triggerTime: '2026-07-27 12:00:00',
    confirmTime: null,
    resolveTime: null,
    handler: null,
    remark: null,
  },
  {
    id: 5,
    level: 'medium',
    status: 'confirmed',
    content: 'GIS室SF6气体泄漏告警（浓度35ppm，阈值30ppm）',
    plantId: 2,
    triggerTime: '2026-07-27 10:45:00',
    confirmTime: '2026-07-27 11:00:00',
    resolveTime: null,
    handler: '王刚',
    remark: null,
  },
];

// ---------------------------------------------------------------------------
//  Mock Data -- Warnings (3)
// ---------------------------------------------------------------------------
const mockWarnings = [
  {
    id: 1,
    level: 'orange',
    type: 'weather',
    content: '广东省气象台发布台风橙色预警：超强台风"海葵"预计24小时内正面影响珠三角地区，风力12-14级',
    plantId: 2,
    startTime: '2026-07-27 08:00:00',
    endTime: '2026-07-29 08:00:00',
    status: 'active',
    createTime: '2026-07-27 08:00:00',
  },
  {
    id: 2,
    level: 'yellow',
    type: 'flood',
    content: '北江流域持续降雨，石角水文站水位逼近警戒线（当前10.8m，警戒11.5m），预计6小时内达到警戒水位',
    plantId: 1,
    startTime: '2026-07-27 10:00:00',
    endTime: '2026-07-28 10:00:00',
    status: 'active',
    createTime: '2026-07-27 10:00:00',
  },
  {
    id: 3,
    level: 'blue',
    type: 'equipment',
    content: '华电深圳燃气电厂3号燃机振动值持续升高，建议关注',
    plantId: 2,
    startTime: '2026-07-27 06:00:00',
    endTime: '2026-07-27 18:00:00',
    status: 'active',
    createTime: '2026-07-27 06:00:00',
  },
];

// ---------------------------------------------------------------------------
//  Mock Data -- Users
// ---------------------------------------------------------------------------
const mockUsers = [
  {
    id: 1,
    username: 'admin',
    realName: '系统管理员',
    phone: '13800000001',
    email: 'admin@hdgd.com',
    orgId: 1,
    status: 'active',
    createTime: '2024-01-01 00:00:00',
  },
  {
    id: 2,
    username: 'operator',
    realName: '运维操作员',
    phone: '13800000002',
    email: 'operator@hdgd.com',
    orgId: 2,
    status: 'active',
    createTime: '2024-02-15 10:00:00',
  },
  {
    id: 3,
    username: 'zhangwei',
    realName: '张伟',
    phone: '13800000003',
    email: 'zhangwei@hdgd.com',
    orgId: 2,
    status: 'active',
    createTime: '2024-03-10 09:00:00',
  },
];

// ---------------------------------------------------------------------------
//  Mock Data -- Roles
// ---------------------------------------------------------------------------
const mockRoles = [
  { id: 1, code: 'ADMIN', name: '系统管理员', description: '拥有全部权限' },
  { id: 2, code: 'OPERATOR', name: '运维操作员', description: '电厂运维操作权限' },
  { id: 3, code: 'VIEWER', name: '只读用户', description: '仅查看权限' },
];

// ---------------------------------------------------------------------------
//  Mock Data -- Orgs (tree)
// ---------------------------------------------------------------------------
const mockOrgs = [
  {
    id: 1,
    name: '华电广东公司',
    parentId: null,
    sort: 0,
    children: [
      {
        id: 2,
        name: '广州运营中心',
        parentId: 1,
        sort: 1,
        children: [
          { id: 4, name: '广州电厂运维部', parentId: 2, sort: 1, children: [] },
        ],
      },
      {
        id: 3,
        name: '深圳运营中心',
        parentId: 1,
        sort: 2,
        children: [
          { id: 5, name: '深圳电厂运维部', parentId: 3, sort: 1, children: [] },
        ],
      },
    ],
  },
];

// ---------------------------------------------------------------------------
//  Mock Data -- Weather (per plant)
// ---------------------------------------------------------------------------
const mockWeather: Record<number, object> = {
  1: {
    id: 1,
    plantId: 1,
    temp: 33.5,
    humidity: 72.0,
    windSpeed: 3.2,
    windDirection: 'SE',
    rainfall: 0.0,
    recordTime: '2026-07-27 14:00:00',
  },
  2: {
    id: 2,
    plantId: 2,
    temp: 31.8,
    humidity: 85.0,
    windSpeed: 8.5,
    windDirection: 'NE',
    rainfall: 15.2,
    recordTime: '2026-07-27 14:00:00',
  },
  3: {
    id: 3,
    plantId: 3,
    temp: 30.2,
    humidity: 78.0,
    windSpeed: 6.1,
    windDirection: 'E',
    rainfall: 2.3,
    recordTime: '2026-07-27 14:00:00',
  },
};

// ---------------------------------------------------------------------------
//  Mock Data -- Hydro Readings
// ---------------------------------------------------------------------------
function generateHydroReadings(stationId: number): object[] {
  const baseLevel = stationId === 1 ? 10.8 : 10.2;
  const readings: object[] = [];
  for (let i = 0; i < 24; i++) {
    const hour = `${String(i).padStart(2, '0')}:00:00`;
    readings.push({
      id: stationId * 1000 + i,
      stationId,
      waterLevel: parseFloat((baseLevel + Math.sin(i / 3) * 0.5 + (Math.random() - 0.5) * 0.1).toFixed(2)),
      flowRate: parseFloat((1200 + Math.cos(i / 4) * 300 + (Math.random() - 0.5) * 50).toFixed(2)),
      trend: i % 7 < 3 ? 'rising' : i % 7 < 5 ? 'steady' : 'falling',
      alertLevel: baseLevel + Math.sin(i / 3) * 0.5 > 11 ? 'watch' : 'normal',
      readingTime: `2026-07-27 ${hour}`,
    });
  }
  return readings;
}

// ---------------------------------------------------------------------------
//  Mock Data -- Prediction (24 forecast points)
// ---------------------------------------------------------------------------
function generateForecastValues(
  predictType: string,
  targetId: number,
): {
  forecastTimes: string[];
  forecastValues: number[];
  upperBound: number[];
  lowerBound: number[];
} {
  const base = predictType === 'water_level'
    ? (targetId === 1 ? 10.8 : 10.2)
    : 350;
  const amplitude = predictType === 'water_level' ? 1.2 : 80;
  const forecastTimes: string[] = [];
  const forecastValues: number[] = [];
  const upperBound: number[] = [];
  const lowerBound: number[] = [];

  const nowDate = new Date('2026-07-27T15:00:00');
  for (let h = 1; h <= 24; h++) {
    const t = new Date(nowDate.getTime() + h * 3600_000);
    forecastTimes.push(
      `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')} ${String(t.getHours()).padStart(2, '0')}:00:00`,
    );
    const val = base + amplitude * Math.sin(h / 4) + (Math.random() - 0.5) * 0.3;
    forecastValues.push(parseFloat(val.toFixed(2)));
    upperBound.push(parseFloat((val + 0.4).toFixed(2)));
    lowerBound.push(parseFloat((val - 0.4).toFixed(2)));
  }
  return { forecastTimes, forecastValues, upperBound, lowerBound };
}

// ---------------------------------------------------------------------------
//  Mock Data -- Dashboard stats
// ---------------------------------------------------------------------------
const mockDashboard = {
  totalPlants: 3,
  statusDistribution: {
    normal: 2,
    warning: 1,
    danger: 0,
    offline: 0,
  },
  alarmStats: {
    unconfirmedCount: 2,
    todayCount: 5,
    weekCount: 18,
  },
  warningStats: {
    activeCount: 3,
    todayCount: 3,
  },
};

// ---------------------------------------------------------------------------
//  Mock Data -- Plant Trend (times / values)
// ---------------------------------------------------------------------------
function generatePlantTrend(plantId: number, metric: string) {
  const unit = metric === 'load' ? 'MW' : metric === 'output' ? 'MWh' : '℃';
  const base = plantId === 1 ? 480 : plantId === 2 ? 320 : 120;
  const times: string[] = [];
  const values: number[] = [];

  for (let h = 0; h < 24; h++) {
    const hour = `${String(h).padStart(2, '0')}:00:00`;
    times.push(`2026-07-27 ${hour}`);
    values.push(parseFloat((base + Math.sin(h / 3) * 60 + (Math.random() - 0.5) * 20).toFixed(1)));
  }
  return {
    plantId,
    metric,
    unit,
    times,
    values,
  };
}

// ---------------------------------------------------------------------------
//  Mock Data -- Alarm Statistics
// ---------------------------------------------------------------------------
const mockAlarmStats = [
  { level: 'emergency', count: 1 },
  { level: 'high', count: 1 },
  { level: 'medium', count: 2 },
  { level: 'low', count: 1 },
];

// ---------------------------------------------------------------------------
//  Helper: build a GeoJSON FeatureCollection from plants / stations
// ---------------------------------------------------------------------------
function plantsToGeoJSON(plants: typeof mockPlants) {
  return {
    type: 'FeatureCollection' as const,
    features: plants.map((p) => ({
      type: 'Feature' as const,
      geometry: { type: 'Point' as const, coordinates: [p.lng, p.lat] },
      properties: { id: p.id, name: p.name, type: p.type, capacity: p.capacity, status: p.status },
    })),
  };
}

function stationsToGeoJSON(stations: typeof mockHydroStations) {
  return {
    type: 'FeatureCollection' as const,
    features: stations.map((s) => ({
      type: 'Feature' as const,
      geometry: { type: 'Point' as const, coordinates: [s.lng, s.lat] },
      properties: { id: s.id, name: s.name, river: s.river, city: s.city, status: s.status },
    })),
  };
}

// ===========================================================================
//  HANDLERS
// ===========================================================================
export const handlers = [
  // =======================================================================
  //  AUTH -- 认证授权
  // =======================================================================

  /** POST /api/auth/login  (不需要 Token) */
  http.post('http://localhost:8080/api/auth/login', async ({ request }) => {
    await delay(300);
    const body = await request.json() as Record<string, string>;
    const { username, password } = body;

    if (username === 'admin' && password === 'admin123') {
      return ok({
        token: MOCK_TOKEN,
        userId: 1,
        username: 'admin',
        realName: '系统管理员',
        roles: ['ADMIN'],
        permissions: ['*'],
      });
    }

    if (username === 'operator' && password === 'oper123') {
      return ok({
        token: MOCK_TOKEN,
        userId: 2,
        username: 'operator',
        realName: '运维操作员',
        roles: ['OPERATOR'],
        permissions: ['plant:view', 'alarm:view', 'alarm:confirm', 'alarm:resolve', 'warning:view', 'stats:view'],
      });
    }

    return HttpResponse.json(
      { code: 401, msg: '用户名或密码错误', data: null },
      { status: 401 },
    );
  }),

  /** POST /api/auth/logout */
  http.post('http://localhost:8080/api/auth/logout', async () => {
    await delay(100);
    return okWithoutData();
  }),

  /** POST /api/auth/refreshToken */
  http.post('http://localhost:8080/api/auth/refreshToken', async ({ request }) => {
    await delay(200);
    if (!requireAuth(request)) {
      return fail(401, '未携带 Token');
    }
    return ok({
      token: MOCK_TOKEN,
      userId: 1,
      username: 'admin',
      realName: '系统管理员',
      roles: ['ADMIN'],
      permissions: ['*'],
    });
  }),

  // =======================================================================
  //  USERS -- 用户管理
  // =======================================================================

  /** GET /api/users */
  http.get('http://localhost:8080/api/users', async ({ request }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') ?? 1);
    const size = Number(url.searchParams.get('size') ?? 10);
    return ok({
      list: mockUsers,
      total: mockUsers.length,
      page,
      size,
    });
  }),

  /** GET /api/users/:id */
  http.get('http://localhost:8080/api/users/:id', async ({ request, params }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const user = mockUsers.find((u) => u.id === id);
    if (!user) return fail(404, '用户不存在');
    return ok({
      token: MOCK_TOKEN,
      userId: user.id,
      username: user.username,
      realName: user.realName,
      roles: id === 1 ? ['ADMIN'] : id === 2 ? ['OPERATOR'] : ['VIEWER'],
      permissions: id === 1 ? ['*'] : ['plant:view', 'alarm:view'],
    });
  }),

  /** POST /api/users */
  http.post('http://localhost:8080/api/users', async ({ request }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    const body = (await request.json()) as Record<string, unknown>;
    return ok({ id: 999, ...body });
  }),

  /** PUT /api/users */
  http.put('http://localhost:8080/api/users', async ({ request }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    const body = (await request.json()) as Record<string, unknown>;
    return ok(body);
  }),

  /** DELETE /api/users/:id */
  http.delete('http://localhost:8080/api/users/:id', async ({ request }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    return okWithoutData();
  }),

  // =======================================================================
  //  ROLES -- 角色管理
  // =======================================================================

  /** GET /api/roles */
  http.get('http://localhost:8080/api/roles', async ({ request }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    return ok(mockRoles);
  }),

  /** GET /api/roles/:id */
  http.get('http://localhost:8080/api/roles/:id', async ({ request, params }) => {
    await delay(100);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const role = mockRoles.find((r) => r.id === id);
    if (!role) return fail(404, '角色不存在');
    return ok(role);
  }),

  // =======================================================================
  //  ORGS -- 组织架构管理
  // =======================================================================

  /** GET /api/orgs */
  http.get('http://localhost:8080/api/orgs', async ({ request }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    // 平铺列表：递归展开树结构
    const flat: object[] = [];
    function walk(nodes: typeof mockOrgs) {
      for (const node of nodes) {
        const { children, ...rest } = node;
        flat.push(rest);
        if (children?.length) walk(children);
      }
    }
    walk(mockOrgs);
    return ok(flat);
  }),

  /** GET /api/orgs/tree */
  http.get('http://localhost:8080/api/orgs/tree', async ({ request }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    return ok(mockOrgs);
  }),

  // =======================================================================
  //  PLANTS -- 电厂管理
  // =======================================================================

  /** GET /api/plants */
  http.get('http://localhost:8080/api/plants', async ({ request }) => {
    await delay(300);
    if (!requireAuth(request)) return fail(401, '未认证');
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') ?? 1);
    const size = Number(url.searchParams.get('size') ?? 10);
    const type = url.searchParams.get('type');
    const status = url.searchParams.get('status');
    const keyword = url.searchParams.get('keyword');

    let filtered = [...mockPlants];
    if (type) filtered = filtered.filter((p) => p.type === type);
    if (status) filtered = filtered.filter((p) => p.status === status);
    if (keyword) filtered = filtered.filter((p) => p.name.includes(keyword));

    return ok({
      list: filtered,
      total: filtered.length,
      page,
      size,
    });
  }),

  /** GET /api/plants/:id */
  http.get('http://localhost:8080/api/plants/:id', async ({ request, params }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const plant = mockPlants.find((p) => p.id === id);
    if (!plant) return fail(404, '电厂不存在');
    return ok(plant);
  }),

  /** POST /api/plants */
  http.post('http://localhost:8080/api/plants', async ({ request }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    const body = (await request.json()) as Record<string, unknown>;
    return ok({ id: 999, ...body });
  }),

  /** PUT /api/plants/:id */
  http.put('http://localhost:8080/api/plants/:id', async ({ request, params }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const body = (await request.json()) as Record<string, unknown>;
    return ok({ id, ...body });
  }),

  /** DELETE /api/plants/:id */
  http.delete('http://localhost:8080/api/plants/:id', async ({ request }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    return okWithoutData();
  }),

  /** GET /api/plants/nearby */
  http.get('http://localhost:8080/api/plants/nearby', async ({ request }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    return ok(mockPlants);
  }),

  // =======================================================================
  //  HYDRO -- 水文站管理
  // =======================================================================

  /** GET /api/hydro/stations */
  http.get('http://localhost:8080/api/hydro/stations', async ({ request }) => {
    await delay(250);
    if (!requireAuth(request)) return fail(401, '未认证');
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') ?? 1);
    const size = Number(url.searchParams.get('size') ?? 10);
    return ok({
      list: mockHydroStations,
      total: mockHydroStations.length,
      page,
      size,
    });
  }),

  /** GET /api/hydro/stations/:id */
  http.get('http://localhost:8080/api/hydro/stations/:id', async ({ request, params }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const station = mockHydroStations.find((s) => s.id === id);
    if (!station) return fail(404, '水文站不存在');
    return ok(station);
  }),

  /** GET /api/hydro/stations/by-city/:city */
  http.get('http://localhost:8080/api/hydro/stations/by-city/:city', async ({ request, params }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    const city = params.city as string;
    const filtered = mockHydroStations.filter((s) => s.city === city);
    return ok(filtered);
  }),

  // =======================================================================
  //  GIS -- 空间服务
  // =======================================================================

  /** GET /api/gis/plants/bounds */
  http.get('http://localhost:8080/api/gis/plants/bounds', async ({ request }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    return ok(plantsToGeoJSON(mockPlants));
  }),

  /** GET /api/gis/hydro/stations/bounds */
  http.get('http://localhost:8080/api/gis/hydro/stations/bounds', async ({ request }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    return ok(stationsToGeoJSON(mockHydroStations));
  }),

  /** GET /api/gis/distance */
  http.get('http://localhost:8080/api/gis/distance', async ({ request }) => {
    await delay(100);
    if (!requireAuth(request)) return fail(401, '未认证');
    const url = new URL(request.url);
    const lng1 = Number(url.searchParams.get('lng1'));
    const lat1 = Number(url.searchParams.get('lat1'));
    const lng2 = Number(url.searchParams.get('lng2'));
    const lat2 = Number(url.searchParams.get('lat2'));
    // Haversine 简化近似
    const R = 6371;
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLng = ((lng2 - lng1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2;
    const dist = R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return ok(parseFloat(dist.toFixed(2)));
  }),

  // =======================================================================
  //  ALARMS -- 报警管理
  // =======================================================================

  /** GET /api/alarms */
  http.get('http://localhost:8080/api/alarms', async ({ request }) => {
    await delay(300);
    if (!requireAuth(request)) return fail(401, '未认证');
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') ?? 1);
    const size = Number(url.searchParams.get('size') ?? 10);
    const level = url.searchParams.get('level');
    const status = url.searchParams.get('status');
    const plantId = url.searchParams.get('plantId');

    let filtered = [...mockAlarms];
    if (level) filtered = filtered.filter((a) => a.level === level);
    if (status) filtered = filtered.filter((a) => a.status === status);
    if (plantId) filtered = filtered.filter((a) => a.plantId === Number(plantId));

    return ok({
      list: filtered,
      total: filtered.length,
      page,
      size,
    });
  }),

  /** GET /api/alarms/:id */
  http.get('http://localhost:8080/api/alarms/:id', async ({ request, params }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const alarm = mockAlarms.find((a) => a.id === id);
    if (!alarm) return fail(404, '报警不存在');
    return ok(alarm);
  }),

  /** PUT /api/alarms/:id/confirm */
  http.put('http://localhost:8080/api/alarms/:id/confirm', async ({ request, params }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const url = new URL(request.url);
    const handler = url.searchParams.get('handler') ?? '未知';
    const alarm = mockAlarms.find((a) => a.id === id);
    if (!alarm) return fail(404, '报警不存在');
    return ok({
      ...alarm,
      status: 'confirmed',
      confirmTime: now,
      handler,
    });
  }),

  /** PUT /api/alarms/:id/resolve */
  http.put('http://localhost:8080/api/alarms/:id/resolve', async ({ request, params }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const url = new URL(request.url);
    const handler = url.searchParams.get('handler') ?? '未知';
    const remark = url.searchParams.get('remark') ?? '';
    const alarm = mockAlarms.find((a) => a.id === id);
    if (!alarm) return fail(404, '报警不存在');
    return ok({
      ...alarm,
      status: 'resolved',
      confirmTime: alarm.confirmTime ?? now,
      resolveTime: now,
      handler,
      remark,
    });
  }),

  // =======================================================================
  //  WARNINGS -- 预警管理
  // =======================================================================

  /** GET /api/warnings */
  http.get('http://localhost:8080/api/warnings', async ({ request }) => {
    await delay(250);
    if (!requireAuth(request)) return fail(401, '未认证');
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') ?? 1);
    const size = Number(url.searchParams.get('size') ?? 10);
    const level = url.searchParams.get('level');
    const type = url.searchParams.get('type');
    const status = url.searchParams.get('status');
    const plantId = url.searchParams.get('plantId');

    let filtered = [...mockWarnings];
    if (level) filtered = filtered.filter((w) => w.level === level);
    if (type) filtered = filtered.filter((w) => w.type === type);
    if (status) filtered = filtered.filter((w) => w.status === status);
    if (plantId) filtered = filtered.filter((w) => w.plantId === Number(plantId));

    return ok({
      list: filtered,
      total: filtered.length,
      page,
      size,
    });
  }),

  /** GET /api/warnings/:id */
  http.get('http://localhost:8080/api/warnings/:id', async ({ request, params }) => {
    await delay(150);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const warning = mockWarnings.find((w) => w.id === id);
    if (!warning) return fail(404, '预警不存在');
    return ok(warning);
  }),

  /** PUT /api/warnings/:id/cancel */
  http.put('http://localhost:8080/api/warnings/:id/cancel', async ({ request, params }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    const id = Number(params.id);
    const warning = mockWarnings.find((w) => w.id === id);
    if (!warning) return fail(404, '预警不存在');
    return ok({ ...warning, status: 'cancelled' });
  }),

  // =======================================================================
  //  RULES -- 规则管理
  // =======================================================================

  /** POST /api/rules/reload */
  http.post('http://localhost:8080/api/rules/reload', async ({ request }) => {
    await delay(500);
    if (!requireAuth(request)) return fail(401, '未认证');
    return ok({
      success: true,
      ruleVersion: 'v2026.07.27.3',
      timestamp: Date.now(),
    });
  }),

  /** GET /api/rules/version */
  http.get('http://localhost:8080/api/rules/version', async ({ request }) => {
    await delay(100);
    if (!requireAuth(request)) return fail(401, '未认证');
    return ok({
      ruleVersion: 'v2026.07.27.3',
      timestamp: Date.now(),
    });
  }),

  // =======================================================================
  //  STATS -- 统计分析
  // =======================================================================

  /** GET /api/stats/dashboard */
  http.get('http://localhost:8080/api/stats/dashboard', async ({ request }) => {
    await delay(300);
    if (!requireAuth(request)) return fail(401, '未认证');
    return ok(mockDashboard);
  }),

  /** GET /api/stats/plant-trend */
  http.get('http://localhost:8080/api/stats/plant-trend', async ({ request }) => {
    await delay(350);
    if (!requireAuth(request)) return fail(401, '未认证');
    const url = new URL(request.url);
    const plantId = Number(url.searchParams.get('plantId') ?? 1);
    const metric = url.searchParams.get('metric') ?? 'load';
    return ok(generatePlantTrend(plantId, metric));
  }),

  /** GET /api/stats/alarm-statistics */
  http.get('http://localhost:8080/api/stats/alarm-statistics', async ({ request }) => {
    await delay(200);
    if (!requireAuth(request)) return fail(401, '未认证');
    return ok(mockAlarmStats);
  }),

  // =======================================================================
  //  PREDICT -- 预测服务
  // =======================================================================

  /** GET /api/predict/water-level/:stationId */
  http.get('http://localhost:8080/api/predict/water-level/:stationId', async ({ request, params }) => {
    await delay(400);
    if (!requireAuth(request)) return fail(401, '未认证');
    const stationId = Number(params.stationId);
    const url = new URL(request.url);
    const forecastHours = Number(url.searchParams.get('forecastHours') ?? 24);
    const modelType = url.searchParams.get('modelType') ?? 'lstm';
    const { forecastTimes, forecastValues, upperBound, lowerBound } = generateForecastValues('water_level', stationId);
    return ok({
      predictType: 'water_level',
      targetId: stationId,
      forecastTimes: forecastTimes.slice(0, forecastHours),
      forecastValues: forecastValues.slice(0, forecastHours),
      upperBound: upperBound.slice(0, forecastHours),
      lowerBound: lowerBound.slice(0, forecastHours),
      confidence: 0.95,
      modelUsed: modelType,
      fallback: false,
      generatedAt: '2026-07-27 15:00:00',
    });
  }),

  /** GET /api/predict/power/:plantId */
  http.get('http://localhost:8080/api/predict/power/:plantId', async ({ request, params }) => {
    await delay(400);
    if (!requireAuth(request)) return fail(401, '未认证');
    const plantId = Number(params.plantId);
    const url = new URL(request.url);
    const forecastHours = Number(url.searchParams.get('forecastHours') ?? 24);
    const modelType = url.searchParams.get('modelType') ?? 'lstm';
    const { forecastTimes, forecastValues, upperBound, lowerBound } = generateForecastValues('power_generation', plantId);
    return ok({
      predictType: 'power_generation',
      targetId: plantId,
      forecastTimes: forecastTimes.slice(0, forecastHours),
      forecastValues: forecastValues.slice(0, forecastHours),
      upperBound: upperBound.slice(0, forecastHours),
      lowerBound: lowerBound.slice(0, forecastHours),
      confidence: 0.90,
      modelUsed: modelType,
      fallback: false,
      generatedAt: '2026-07-27 15:00:00',
    });
  }),

  // =======================================================================
  //  COLLECTOR -- 数据采集
  // =======================================================================

  /** GET /api/collector/weather/:plantId */
  http.get('http://localhost:8080/api/collector/weather/:plantId', async ({ request, params }) => {
    await delay(250);
    if (!requireAuth(request)) return fail(401, '未认证');
    const plantId = Number(params.plantId);
    const data = mockWeather[plantId];
    if (!data) return fail(404, '电厂气象数据不存在');
    return ok(data);
  }),

  /** GET /api/collector/hydro/:stationId/readings */
  http.get('http://localhost:8080/api/collector/hydro/:stationId/readings', async ({ request, params }) => {
    await delay(300);
    if (!requireAuth(request)) return fail(401, '未认证');
    const stationId = Number(params.stationId);
    return ok(generateHydroReadings(stationId));
  }),
];
