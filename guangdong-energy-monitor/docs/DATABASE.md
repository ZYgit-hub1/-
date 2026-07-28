# 广东省电厂监控平台 - 数据库设计

> 华电集团广东电厂监控平台数据库设计说明文档
> 版本：2.0.0（升级为 PostgreSQL+PostGIS + TDengine 双库架构）
> 更新日期：2026-07-27

---

## 1. 数据库选型

平台采用多类型数据库混合存储策略，针对不同数据特征选择最优存储方案：

| 数据库 | 版本 | 用途 | 部署形态 |
| --- | --- | --- | --- |
| **PostgreSQL** | 15+ | 业务关系型数据（电厂、水文站、预警、规则）+ 空间数据 | 1 主 2 从，流复制 |
| **PostGIS** | 3.4+ | 空间扩展，支持 GIS 空间查询（ST_DWithin / ST_Contains） | 随 PostgreSQL 安装 |
| **TDengine** | 3.2+ | 高频时序数据（气象/水文实时采集数据） | 3 节点集群 |
| Redis | 7.0 | 热点缓存、分布式锁、会话、实时指标 | 1 主 2 从 3 哨兵 |
| Elasticsearch | 8.x | 日志检索（ELK） | 3 节点集群 |

**选型说明：**

- **PostgreSQL + PostGIS** 承载核心业务数据，PostGIS 提供专业的空间索引（GiST）与空间函数，支撑电厂/水文站地图分布查询、半径搜索、行政区划过滤等 GIS 场景。
- **TDengine** 专门存储高频时序数据（气象每分钟、水文每 5 分钟），利用其超级表（STable）+ 子表架构，单节点写入吞吐 > 100 万条/秒，支持降采样、连续查询预聚合。
- **Redis** 缓存电厂列表、用户权限、实时看板数据，降低 PostgreSQL 压力。
- 业务表统一使用 PostgreSQL 枚举类型（`power_plant_type`、`plant_status` 等），保证数据一致性。

---

## 2. 数据库分工

### 2.1 PostgreSQL（业务库 + 空间库）

| 表名 | 说明 | 空间列 |
| --- | --- | --- |
| `t_power_plant` | 电厂基础信息 | `location geometry(Point,4326)` |
| `t_hydro_station` | 水文站基础信息 | `location geometry(Point,4326)` |
| `t_weather_warning` | 气象预警（按月分区） | 无（按 `district_code` 索引） |
| `t_alert_rule` | 报警规则配置（驱动 Drools） | 无（JSONB 存储条件与目标） |

### 2.2 TDengine（时序库）

| 超级表 | 说明 | TAG | 列 |
| --- | --- | --- | --- |
| `weather_live` | 气象实时数据 | `plant_id`, `district_code` | `ts`, `temp`, `humidity`, `wind_speed`, `wind_dir`, `rain_1h`, `pressure` |
| `hydro_level` | 水文实时数据 | `station_id` | `ts`, `water_level`, `flow`, `is_over_warning` |

---

## 3. PostgreSQL + PostGIS DDL 详解

> 完整 DDL 见 `docs/ddl/01_postgresql_postgis.sql`

### 3.1 扩展与枚举

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS btree_gist;  -- 时间+空间联合索引
CREATE EXTENSION IF NOT EXISTS pg_trgm;     -- 模糊查询

CREATE TYPE power_plant_type     AS ENUM ('coal','gas','solar','wind','storage');
CREATE TYPE plant_status         AS ENUM ('normal','warning','danger','offline');
CREATE TYPE weather_warning_type AS ENUM ('typhoon','rainstorm','high_temp','gale','fog','haze','thunder','ice','drought','flood','other');
CREATE TYPE warning_level        AS ENUM ('blue','yellow','orange','red');
CREATE TYPE alert_rule_type      AS ENUM ('hydro','weather','fire','equipment','composite');
```

### 3.2 t_power_plant 电厂表

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigserial | 主键 |
| name | varchar(120) | 电厂名称（唯一） |
| type | power_plant_type | 电厂类型枚举 |
| location | geometry(Point,4326) | WGS84 经纬度空间点 |
| district_code | varchar(12) | 行政区划代码 |
| river_basin | varchar(60) | 所属流域 |
| capacity | numeric(10,2) | 装机容量（MW） |
| status | plant_status | 运行状态枚举 |
| address | varchar(255) | 详细地址 |
| create_time | timestamptz | 创建时间（带时区） |
| update_time | timestamptz | 更新时间（触发器自动维护） |
| is_deleted | smallint | 逻辑删除 |

**索引策略：**
- `idx_power_plant_location`：GiST 空间索引，加速 `ST_DWithin` / `ST_Contains`
- `uk_power_plant_name`：部分唯一索引（`WHERE is_deleted = 0`）
- `idx_plant_type_status`：复合索引，大屏分类统计
- `idx_plant_district`：行政区划聚合
- `idx_plant_river_basin`：流域关联分析

### 3.3 t_hydro_station 水文站表

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigserial | 主键 |
| name | varchar(120) | 水文站名称（唯一） |
| river_basin | varchar(60) | 所属河流/流域 |
| location | geometry(Point,4326) | WGS84 经纬度 |
| warning_level | numeric(6,2) | 警戒水位（m） |
| guarantee_level | numeric(6,2) | 保证水位（m） |
| historical_max | numeric(6,2) | 历史最高水位（m） |
| flow_capacity | numeric(10,2) | 测流能力（m³/s） |
| city | varchar(50) | 所在城市 |
| status | plant_status | 运行状态 |

### 3.4 t_weather_warning 气象预警表（按月分区）

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigserial | 主键 |
| district_code | varchar(12) | 行政区划代码 |
| type | weather_warning_type | 预警类型枚举 |
| level | warning_level | 预警级别枚举 |
| publish_time | timestamptz | 发布时间（分区键） |
| expire_time | timestamptz | 失效时间 |
| content_json | jsonb | 预警详情（GIN 索引支持检索） |
| source | varchar(60) | 数据来源 |
| status | varchar(20) | active/expired/cancelled |

**分区策略：** `PARTITION BY RANGE (publish_time)`，按月分区，通过 pg_partman 自动维护。

### 3.5 t_alert_rule 报警规则表

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| id | bigserial | 主键 |
| name | varchar(120) | 规则名称（唯一） |
| rule_type | alert_rule_type | 规则类型枚举 |
| condition_json | jsonb | 规则条件（驱动 Drools） |
| priority | smallint | 优先级 1-100（越小越高） |
| targets_json | jsonb | 作用目标 `{plant_ids, station_ids, districts}` |
| dead_zone | numeric(10,2) | 死区阈值（防抖动） |
| delay_sec | integer | 延迟触发秒数 |
| enabled | boolean | 是否启用 |
| version | integer | 乐观锁版本号 |

### 3.6 空间查询函数

```sql
-- 查询经纬度半径 R 米内的电厂（走 GiST 索引）
SELECT * FROM find_plants_within_radius(113.2644, 23.1291, 50000);

-- 查询某行政区划内的电厂
SELECT * FROM find_plants_in_district('440100');
```

---

## 4. TDengine DDL 详解

> 完整 DDL 见 `docs/ddl/02_tdengine.sql`

### 4.1 数据库配置

```sql
CREATE DATABASE energy_monitor
    PRECISION 'ms'    -- 毫秒精度
    KEEP 3650          -- 保留 10 年
    DURATION 10        -- 每 10 天一个数据文件
    CACHELAST 1;       -- 缓存最新值，加速 LAST_ROW()
```

### 4.2 weather_live 气象实时超级表

```sql
CREATE STABLE weather_live (
    ts         TIMESTAMP,   -- 采集时间
    temp       FLOAT,       -- 温度（℃）
    humidity   FLOAT,       -- 湿度（%）
    wind_speed FLOAT,       -- 风速（m/s）
    wind_dir   SMALLINT,    -- 风向（0-359°）
    rain_1h    FLOAT,       -- 过去1小时降雨量（mm）
    pressure   FLOAT        -- 气压（hPa）
)
TAGS (
    plant_id      BIGINT,      -- 电厂 ID
    district_code VARCHAR(12)  -- 行政区划代码
);
```

### 4.3 hydro_level 水文实时超级表

```sql
CREATE STABLE hydro_level (
    ts              TIMESTAMP,  -- 采集时间
    water_level     FLOAT,      -- 水位（m）
    flow            FLOAT,      -- 流量（m³/s）
    is_over_warning BOOL         -- 是否超警戒
)
TAGS (
    station_id BIGINT           -- 水文站 ID
);
```

### 4.4 索引策略说明

TDengine 无需显式建索引，性能优化机制：
1. **TAG 内存索引**：所有 TAG 列自动建索引，`WHERE plant_id = 1` 高效过滤
2. **时间分片**：数据按时间分片存储，时间范围查询天然高效
3. **列式存储**：查询指定列时 IO 放大极小
4. **降采样**：`INTERVAL()` + 连续查询（CQ）预聚合
5. **缓存最新值**：`CACHELAST=1`，`LAST_ROW()` 查询 O(1)

### 4.5 连续查询（预聚合）

```sql
-- 每 5 分钟将气象原始数据降采样为 5 分钟均值
CREATE CONTINUOUS QUERY cq_weather_5m ON energy_monitor
RESAMPLE EVERY 5s
SELECT AVG(temp), MAX(temp), MIN(temp), AVG(humidity), AVG(wind_speed), SUM(rain_1h)
FROM weather_live
INTERVAL(5m)
GROUP BY plant_id, district_code
INTO energy_monitor_agg.weather_5m;
```

---

## 5. 实体与 Repository 映射

### 5.1 PostgreSQL 实体（MyBatis-Plus + JPA 双栈）

| 表 | MyBatis-Plus Entity | JPA Repository | MyBatis-Plus Mapper |
| --- | --- | --- | --- |
| t_power_plant | `PowerPlant` | `PowerPlantRepository` | `PowerPlantMapper` |
| t_hydro_station | `HydroStation` | - | `HydroStationMapper` |
| t_weather_warning | `WeatherWarning` | - | `WeatherWarningMapper` |
| t_alert_rule | `AlertRule` | - | `AlertRuleMapper` |

**关键设计：**
- 枚举字段使用 MyBatis-Plus `@EnumValue` 注解，自动映射 PostgreSQL ENUM 类型
- 空间字段 `location` 使用自定义 `GeometryPointTypeHandler`，实现 JTS `Point` ↔ PostGIS `geometry` 双向转换
- JSONB 字段映射为 `String`，复杂查询使用 `@>` 操作符

### 5.2 TDengine 实体与 Repository

| 超级表 | Entity | Repository |
| --- | --- | --- |
| weather_live | `WeatherLive` | `TdengineRepository` |
| hydro_level | `HydroLevel` | `TdengineRepository` |

**TDengine JDBC Connector 写入方式：**
```java
// 自动建子表 + 单条写入
INSERT INTO weather_live_1 USING weather_live TAGS (1, '440100')
VALUES (?, ?, ?, ?, ?, ?, ?)

// 批量写入（推荐，吞吐 > 10万条/秒）
INSERT INTO weather_live_1 USING weather_live TAGS (1, '440100') VALUES (?,...),(?,...),...
```

---

## 6. 命名规范

### 6.1 表命名
- 业务表以 `t_` 前缀，如 `t_power_plant`、`t_alert_rule`
- 权限相关表以 `t_sys_` 前缀

### 6.2 字段命名
- 小写字母 + 下划线，避免缩写歧义
- 布尔类型以 `is_` 前缀，如 `is_deleted`、`is_over_warning`
- 时间字段使用 `timestamptz`（带时区），业务时间用语义命名（`publish_time`、`trigger_time`）

### 6.3 主键规范
- 主键统一为 `id`，类型 `bigserial`（PostgreSQL）或由应用层雪花 ID 生成

---

## 7. DDL 脚本索引

| 脚本 | 说明 |
| --- | --- |
| `docs/ddl/01_postgresql_postgis.sql` | PostgreSQL + PostGIS 建表、索引、触发器、函数、初始化数据 |
| `docs/ddl/02_tdengine.sql` | TDengine 超级表、子表、连续查询、示例查询 |

---

## 8. 数据归档

超过 1 年的 TDengine 数据通过 `KEEP` 参数自动过期删除，归档策略：
- 热数据（近 3 个月）：TDengine 原始数据
- 温数据（3 个月-1 年）：TDengine 降采样表（`weather_5m`、`hydro_1m`）
- 冷数据（1 年以上）：导出至 ClickHouse 归档库，用于长期趋势分析
