# 电厂监控管理系统

基于 Vue 3 + TypeScript + Vite 的广东省电厂监控管理平台。

## 技术栈

- **前端框架**: Vue 3 (Composition API)
- **构建工具**: Vite 5
- **类型检查**: TypeScript 5
- **路由管理**: Vue Router 4
- **状态管理**: Pinia
- **UI 组件**: Element Plus
- **图表库**: ECharts 5
- **地图服务**: 高德地图 JS API
- **3D 可视化**: Three.js
- **样式框架**: Tailwind CSS

## 项目结构

```
power-plant-monitor/
├── src/
│   ├── api/              # API 接口封装
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── composables/      # 组合式函数
│   ├── mock/             # Mock 数据
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理
│   ├── types/            # TypeScript 类型定义
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   ├── App.vue           # 根组件
│   ├── main.ts           # 入口文件
│   └── style.css         # 全局样式
├── public/               # 公共资源
├── index.html            # HTML 模板
├── vite.config.ts        # Vite 配置
├── tailwind.config.js    # Tailwind 配置
├── tsconfig.json         # TypeScript 配置
└── package.json          # 依赖管理
```

## 主要功能

### 1. 首页 - GIS地图总览
- 广东省电厂分布地图
- 按预警等级筛选
- 按电厂类型筛选
- 动态标记着色
- 点击查看详情

### 2. 电厂详情页
- **气象信息**: 温度、湿度、风速等实时数据及趋势图
- **水文状态**: 水位、流量监测及历史曲线
- **应急响应**: 应急预案和响应措施
- **实时监测**: 发电功率、设备状态等

### 3. 报警中心
- 报警列表（未确认/已确认/已处置）
- 报警详情和处置记录
- 一键确认和处置

### 4. 预警列表
- 预警等级筛选
- 预警类型筛选
- 预警状态管理

### 5. 数据统计
- 电厂类型分布
- 报警级别分布
- 趋势分析图表

### 6. 3D可视化（可选）
- Three.js 电厂3D模型展示
- 交互式视角控制

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 配置说明

### 高德地图配置

1. 访问 [高德开放平台](https://lbs.amap.com/)
2. 创建应用获取 Web JS API Key
3. 在 `src/composables/useMap.ts` 中替换 `YOUR_AMAP_KEY`

### API 代理配置

在 `vite.config.ts` 中配置代理：

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://your-backend-url',
      changeOrigin: true
    }
  }
}
```

## 开发指南

### 添加新页面

1. 在 `src/views/` 创建页面组件
2. 在 `src/router/index.ts` 中添加路由
3. 使用路由懒加载优化性能

```typescript
{
  path: '/new-page',
  name: 'NewPage',
  component: () => import('@/views/NewPage.vue')
}
```

### 添加新组件

1. 在 `src/components/` 创建组件
2. 使用 Composition API
3. 遵循组件命名规范

### TypeScript 类型定义

在 `src/types/index.ts` 中添加新的类型定义。

## 性能优化

- ✅ 路由懒加载
- ✅ 组件按需加载
- ✅ Gzip 压缩配置
- ✅ 地图实例销毁优化
- ✅ ECharts 懒加载

## 浏览器兼容性

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## License

MIT
