# 广东省电厂监控平台 - 系统架构

> 华电集团广东电厂监控平台技术架构说明文档
> 版本：1.0.0
> 更新日期：2026-07-27

---

## 1. 项目概述

广东省电厂监控平台是华电集团广东区域电厂的统一监控与预警系统，覆盖火电（燃煤/燃气）、新能源（光伏/风电）、储能等多种电厂类型。平台通过实时采集电厂运行数据、气象数据、水文数据、应急事件等多源数据，结合 Drools 规则引擎与 Flink 流处理，实现异常报警、趋势分析与负荷预测，为电厂安全生产与调度决策提供支撑。

**核心能力：**

- 电厂全景监控（运行状态、装机容量、地理位置分布）
- 多源数据采集（气象、水文、应急、设备运行）
- 智能报警（基于 Drools 规则引擎，支持多级预警与处置闭环）
- 负荷与新能源出力预测（对接 Python 机器学习预测服务）
- 统一权限与审计（RBAC、操作审计、数据加密）

**技术基线：**

| 项目 | 版本 |
| --- | --- |
| JDK | Java 17 |
| Spring Boot | 3.2.5 |
| Spring Cloud | 2023.0.1 |
| Spring Cloud Alibaba | 2023.0.1.0 |
| 构建工具 | Maven（多模块） |
| 基础设施 | Docker + Kubernetes |

---

## 2. 技术栈

| 领域 | 技术选型 | 说明 |
| --- | --- | --- |
| 微服务框架 | Spring Cloud 2023.0.1 + Spring Boot 3.2.5 | 基于 Java 17 |
| 网关 | Spring Cloud Gateway | 统一入口、路由、鉴权、限流 |
| 认证授权 | OAuth2 + JWT | Spring Security OAuth2 Resource Server |
| 注册中心 | Nacos 2.3.x | 服务注册与发现 |
| 配置中心 | Nacos Config | 动态配置、灰度发布 |
| 服务间调用 | OpenFeign | 声明式 HTTP 调用，集成 Sentinel 熔断 |
| 限流熔断 | Sentinel | 流控、熔断、热点参数限流 |
| 规则引擎 | Drools 8.x | 报警规则、预警规则动态编排 |
| 流处理 | Apache Flink 1.18 | 实时数据流处理、窗口聚合 |
| 消息队列 | Apache Kafka 3.6 | 采集数据解耦、削峰填谷 |
| 缓存 | Redis 7.0 + Redisson | 分布式锁、布隆过滤器、热点缓存 |
| 数据库 | MySQL 8.0 + MyBatis-Plus | 主从复制，业务数据持久化 |
| 时序数据库 | InfluxDB 2.x | 传感器高频时序数据 |
| 文档 | Springdoc OpenAPI 3.0 | 自动生成 API 文档 |
| 监控 | Spring Boot Admin + Prometheus + Grafana | 应用与指标监控 |
| 链路追踪 | SkyWalking | 全链路追踪 |
| 日志 | ELK（Elasticsearch + Logstash + Kibana） | 日志采集与检索 |
| 容器化 | Docker + Kubernetes | 编排、弹性伸缩 |

---

## 3. 模块划分

项目采用 Maven 多模块结构，共 7 个核心模块：

| 模块 | 职责 | 对外端口 | 依赖中间件 |
| --- | --- | --- | --- |
| `common` | 公共基础模块：通用工具、统一返回结构 `Result`、异常处理、常量、DTO、Feign 客户端定义、安全工具、Redis 配置 | 无（被其他模块依赖） | 无 |
| `gateway` | API 网关：路由转发、JWT 鉴权、OAuth2、接口签名校验、全局限流、跨域处理、灰度路由 | 8080 | Nacos、Redis |
| `service-collector` | 数据采集服务：对接气象 API、水文站、应急平台，采集并标准化数据后写入 Kafka；调度定时采集任务 | 8081 | Nacos、Kafka、Redis、MySQL |
| `service-alert` | 报警与预警服务：消费 Kafka 数据，经 Drools 规则引擎匹配生成报警/预警；支持报警确认、处置、推送 | 8082 | Nacos、Kafka、MySQL、Redis、Drools |
| `service-analysis` | 分析服务：消费 Kafka 数据，Flink 流处理聚合，对接 Python 预测服务做负荷/出力预测，生成趋势统计 | 8083 | Nacos、Kafka、Flink、Redis、Python 预测服务 |
| `service-plant` | 电厂管理服务：电厂基础信息、设备台账、运行状态、地图分布 | 8084 | Nacos、MySQL、Redis |
| `service-user` | 用户与权限服务：用户、角色、权限、组织架构、登录认证、审计日志 | 8085 | Nacos、MySQL、Redis |

---

## 4. 架构图

### 4.1 总体架构

```
                         +-------------------+
                         |      前端应用      |
                         | (Vue3 / 大屏可视化)|
                         +---------+---------+
                                   |
                                   | HTTPS
                                   v
                         +-------------------+
                         |   Gateway (8080)  |
                         | JWT鉴权 / 限流 / 路由|
                         +---------+---------+
                                   |
            +----------------------+----------------------+----------------------+
            |                      |                      |                      |
            v                      v                      v                      v
   +-----------------+    +-----------------+    +-----------------+    +-----------------+
   | service-user    |    | service-plant   |    | service-alert   |    | service-analysis|
   | (8085)          |    | (8084)          |    | (8082)          |    | (8083)          |
   +--------+--------+    +--------+--------+    +--------+--------+    +--------+--------+
            |                      |                      |                      |
            |                      |                      |                      |
            v                      v                      v                      v
        +------+               +------+               +------+               +------+
        |MySQL |               |MySQL |               |MySQL |               |Redis |
        +------+               +------+               +------+               +------+
                                                              ^                ^
                                                              |                |
   +-----------------+                                        |                |
   |service-collector|                                        |                |
   | (8081)          |                                        |                |
   +--------+--------+                                        |                |
            |                                                 |                |
            | 采集气象/水文/应急数据                              |                |
            v                                                 |                |
        +----------+        +----------+        +-----------+  |          +-----------+
        | 气象 API |        | 水文站    |        |  Kafka    |--+          | Flink     |
        +----------+        +----------+        +-----+-----+             | (流处理)  |
                                                |                       +-----+-----+
                                                |                             |
                                                | 消费                        | 对接
                                                v                             v
                                        +-------------+               +-----------------+
                                        |service-alert|               | Python 预测服务  |
                                        | (Drools规则)|               | (负荷/出力预测)  |
                                        +------+------+               +-----------------+
                                               |
                                               v
                                        +-------------+
                                        | 报警推送     |
                                        | (短信/邮件/  |
                                        |  站内信)     |
                                        +-------------+
```

### 4.2 采集与报警链路

```
气象API / 水文站 / 应急平台
        |
        | 定时/实时采集
        v
+------------------+
| service-collector|  数据清洗 + 标准化
+--------+---------+
         |
         | produce
         v
   +-----------+     +------------------+     +------------------+
   |  Kafka    |---->| service-alert    |---->| Drools 规则引擎  |
   |  Topic    |     | (消费)            |     | (阈值/组合规则)  |
   +-----------+     +--------+---------+     +--------+---------+
                              |                        |
                              | 匹配命中                | 推送
                              v                        v
                     +-----------------+      +-----------------+
                     | t_alarm / t_warning|    | 报警推送通道     |
                     | (MySQL 持久化)    |      | 短信/邮件/站内信 |
                     +-----------------+      +-----------------+
```

### 4.3 预测分析链路

```
+------------------+      +-----------+      +------------------+      +-----------------+
| service-collector|----->|   Kafka   |----->| service-analysis |----->| Python 预测服务  |
| (气象/负荷数据)   |      | (数据流)  |      | (Flink 流处理)   |      | (机器学习模型)   |
+------------------+      +-----------+      +--------+---------+      +--------+--------+
                                                     |                           |
                                                     | 聚合结果                   | 预测结果
                                                     v                           v
                                              +---------------+         +-----------------+
                                              | Redis (热点)  |<--------| 预测结果回写     |
                                              | MySQL (持久化)|         +-----------------+
                                              +---------------+
```

---

## 5. 数据流

### 5.1 采集链路

1. `service-collector` 通过定时任务（XXL-Job 调度）与 WebSocket 长连接，分别对接气象 API、水文监测站、应急平台。
2. 采集到的原始数据经过格式标准化、单位换算、异常值过滤后，序列化为统一 JSON 投递到 Kafka Topic（如 `topic.weather`、`topic.hydro`、`topic.emergency`）。
3. Kafka 按分区（电厂 ID hash）分布数据，保证同一电厂数据有序。
4. 高频传感器数据（秒级）额外写入 InfluxDB 时序库，供明细查询。

### 5.2 报警链路

1. `service-alert` 订阅 Kafka 各数据 Topic。
2. 数据进入 Drools KieSession，按规则集（阈值规则、组合规则、趋势规则）进行匹配。
3. 命中规则即生成报警记录（`t_alarm`）或预警记录（`t_warning`），写入 MySQL 并同步缓存。
4. 触发推送：根据报警级别与订阅关系，通过短信、邮件、站内信、WebSocket 实时推送到前端大屏。
5. 运维人员在前端确认报警（`/api/alarms/{id}/confirm`）并处置（`/api/alarms/{id}/resolve`），形成闭环。

### 5.3 预测链路

1. `service-analysis` 消费 Kafka 负荷与气象数据流，经 Flink 窗口聚合（滑动窗口）生成特征向量。
2. 通过 OpenFeign 调用 Python 预测服务（Flask/FastAPI 部署的机器学习模型），输入历史负荷 + 气象特征，输出未来 24h/72h 负荷与新能源出力预测。
3. 预测结果回写 MySQL 与 Redis，供前端趋势看板与 `/api/stats/plant-trend` 接口查询。
4. 预测误差超阈值时自动触发模型重训练通知。

---

## 6. 部署架构

### 6.1 容器化与编排

- 所有微服务构建为 Docker 镜像，推送到私有 Harbor 镜像仓。
- Kubernetes 集群统一编排，每个微服务独立 Deployment，支持 HPA 水平自动伸缩。
- 通过 Ingress 统一对外暴露，Gateway 作为流量入口。

### 6.2 中间件集群

| 中间件 | 部署形态 | 说明 |
| --- | --- | --- |
| Nacos | 3 节点集群 | 注册中心 + 配置中心高可用 |
| MySQL | 1 主 2 从 | 主写从读，MHA 自动故障转移 |
| Redis | 1 主 2 从 3 哨兵 | Redisson 客户端，哨兵自动切换 |
| Kafka | 3 Broker 集群 | 副本因子 2，分区按电厂规模规划 |
| Flink | JobManager + TaskManager 集群 | 部署于 K8s，独立资源池 |
| Elasticsearch | 3 节点集群 | ELK 日志存储 |
| InfluxDB | 单节点（可扩展集群版） | 高频时序数据 |

### 6.3 环境划分

- 开发环境（dev）、测试环境（test）、预发环境（staging）、生产环境（prod），通过 Nacos namespace 隔离配置。

---

## 7. 安全设计

### 7.1 统一鉴权

- 所有对外接口必须经过 Gateway，Gateway 集成 Spring Security OAuth2 Resource Server，校验 JWT Token（RS256 签名）。
- Token 由 `service-user` 签发，包含用户 ID、角色、组织、过期时间等声明。
- 无效或过期 Token 直接返回 401，权限不足返回 403。

### 7.2 RBAC 权限模型

- 基于"用户-角色-权限"三级模型：`t_sys_user` - `t_sys_user_role` - `t_sys_role` - `t_sys_role_permission` - `t_sys_permission`。
- 权限粒度到按钮/接口级别，前端菜单与按钮按权限动态渲染。
- 数据权限按组织架构（`t_sys_org`）隔离，用户仅能查看本组织及下级组织数据。

### 7.3 接口安全

- 对外接口支持 HMAC-SHA256 签名校验（timestamp + nonce + sign），防篡改防重放。
- 敏感字段（如密码、手机号）传输与存储加密，密码采用 BCrypt 加盐。
- 所有接口 HTTPS 传输，关键操作记录审计日志（`t_audit_log`）。

### 7.4 数据加密

- 数据库连接启用 SSL。
- 敏感配置（数据库密码、密钥）通过 Nacos 加密配置存储。
- 审计日志与备份数据加密落盘。

---

## 8. 监控告警

### 8.1 链路追踪

- 采用 Apache SkyWalking 对全链路进行追踪，自动采集 HTTP、Feign、Kafka、MySQL、Redis 调用。
- 请求 TraceId 贯穿 Gateway -> 微服务 -> 中间件，便于问题定位。

### 8.2 指标监控

- 各微服务集成 Micrometer + Prometheus client，暴露 `/actuator/prometheus` 指标端点。
- Prometheus 定时拉取指标，Grafana 可视化展示：QPS、响应时间、JVM、线程池、Kafka 消费延迟等。
- Spring Boot Admin 监控应用健康状态、日志级别动态调整。

### 8.3 日志体系

- ELK 统一日志：各服务通过 Logback 输出 JSON 格式日志，Filebeat 采集后送入 Logstash，存储至 Elasticsearch，Kibana 检索。
- TraceId 注入日志 MDC，实现日志与链路关联。
- 关键业务日志（登录、报警处置）同步写入审计表。

### 8.4 告警策略

- Prometheus Alertmanager 配置告警规则（CPU > 80%、服务宕机、Kafka 积压等）。
- 告警通过钉钉/短信/邮件通知运维人员，与业务报警通道分离。
