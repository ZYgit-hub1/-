# 广东省电厂监控大屏

基于水系图底图的电厂监控可视化大屏，包含 9 座华电广东电厂、6 个水文站的实时分布与预警状态展示。

## 启动方式

这个大屏是纯静态 HTML 页面（无需 npm install），用本地静态服务器启动即可。

预览地址：`http://localhost:8765/pages/dashboard.html`

服务器根目录：`.\html\energy-monitor-dashboard`

### Python（推荐，无需安装额外依赖）

```
python -m http.server 8765 --directory "f:\html\energy-monitor-dashboard"
```

然后浏览器访问 `http://localhost:8765/pages/dashboard.html`

### Node serve

```
npx serve "f:\html\energy-monitor-dashboard" -l 8765
```

## 注意事项

- 页面依赖三个 CDN 资源（Tailwind、Lucide 图标、Chart.js），首次打开需要联网加载
- 大屏按 1920×1080 设计，建议浏览器全屏（F11）查看
- 水系图底图路径为 `../assets/guangdong-hydro-map.png`，必须通过 HTTP 服务器访问（直接双击打开 HTML 会导致相对路径和 CDN 资源加载异常）
