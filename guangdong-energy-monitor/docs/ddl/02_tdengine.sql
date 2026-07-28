-- ============================================================================
-- 广东省电厂监控平台 - TDengine 3.2.x DDL
-- 高频时序数据：气象实时数据、水文实时数据
-- 超级表（STable）+ 子表（每个电厂/水文站一张子表）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. 数据库创建
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS energy_monitor
    PRECISION 'ms'          -- 时间戳精度：毫秒
    KEEP 3650                -- 数据保留 10 年（热数据 + 冷数据分级存储）
    DURATION 10              -- 每 10 天一个数据文件
    BUFFER 16                -- 写缓冲区大小（MB）
    PAGES 256                -- VNode 申明系统页大小
    MINROWS 100              -- 每个文件块最小记录数
    MAXROWS 4096             -- 每个文件块最大记录数
    CACHELAST 1;             -- 缓存最后一条记录（加速最新值查询）

USE energy_monitor;

-- ----------------------------------------------------------------------------
-- 1. 超级表 weather_live  气象实时数据
--    tags: plant_id（电厂ID）, district_code（行政区划代码）
--    columns: ts, temp, humidity, wind_speed, wind_dir, rain_1h, pressure
-- ----------------------------------------------------------------------------
CREATE STABLE IF NOT EXISTS weather_live (
    ts         TIMESTAMP,     -- 数据时间戳（采集时间）
    temp       FLOAT,         -- 温度（℃）
    humidity   FLOAT,         -- 湿度（%）
    wind_speed FLOAT,         -- 风速（m/s）
    wind_dir   SMALLINT,      -- 风向（0-359°）
    rain_1h    FLOAT,         -- 过去 1 小时降雨量（mm）
    pressure   FLOAT          -- 气压（hPa）
)
TAGS (
    plant_id      BIGINT,     -- 电厂 ID（关联 t_power_plant.id）
    district_code VARCHAR(12) -- 行政区划代码（关联 t_power_plant.district_code）
)
COMMENT '气象实时数据超级表（按电厂分子表，tags 支持按电厂/区域聚合）';

-- 列注释（TDengine 3.2+ 支持）
ALTER STABLE weather_live COMMENT COLUMN ts         '数据采集时间戳（毫秒精度）';
ALTER STABLE weather_live COMMENT COLUMN temp       '环境温度（℃，保留 1 位小数）';
ALTER STABLE weather_live COMMENT COLUMN humidity   '相对湿度（%，0-100）';
ALTER STABLE weather_live COMMENT COLUMN wind_speed '风速（m/s，保留 1 位小数）';
ALTER STABLE weather_live COMMENT COLUMN wind_dir   '风向角度（0-359°，0=正北，顺时针）';
ALTER STABLE weather_live COMMENT COLUMN rain_1h    '过去 1 小时累计降雨量（mm）';
ALTER STABLE weather_live COMMENT COLUMN pressure   '大气压（hPa，保留 1 位小数）';
ALTER STABLE weather_live COMMENT COLUMN plant_id   '电厂 ID（关联 PostgreSQL t_power_plant.id）';
ALTER STABLE weather_live COMMENT COLUMN district_code '行政区划代码（如 440100 广州市）';

-- ----------------------------------------------------------------------------
-- 2. 超级表 hydro_level  水文实时数据
--    tags: station_id（水文站ID）
--    columns: ts, water_level, flow, is_over_warning
-- ----------------------------------------------------------------------------
CREATE STABLE IF NOT EXISTS hydro_level (
    ts               TIMESTAMP,  -- 数据时间戳
    water_level      FLOAT,      -- 水位（m）
    flow             FLOAT,      -- 流量（m³/s）
    is_over_warning  BOOL        -- 是否超警戒水位（true=超警戒，false=正常）
)
TAGS (
    station_id       BIGINT      -- 水文站 ID（关联 t_hydro_station.id）
)
COMMENT '水文实时数据超级表（按水文站分子表）';

ALTER STABLE hydro_level COMMENT COLUMN ts               '数据采集时间戳（毫秒精度）';
ALTER STABLE hydro_level COMMENT COLUMN water_level      '实时水位（m，保留 2 位小数）';
ALTER STABLE hydro_level COMMENT COLUMN flow             '实时流量（m³/s，保留 1 位小数）';
ALTER STABLE hydro_level COMMENT COLUMN is_over_warning  '是否超警戒水位（true=超警戒，触发报警判断）';
ALTER STABLE hydro_level COMMENT COLUMN station_id       '水文站 ID（关联 PostgreSQL t_hydro_station.id）';

-- ----------------------------------------------------------------------------
-- 3. 自动建子表示例（可选，通常由应用层写入时自动创建）
--    规则：{stable}_{plant_id} / {stable}_{station_id}
-- ----------------------------------------------------------------------------

-- 3.1 气象子表：weather_live_1（电厂 ID=1）
CREATE TABLE IF NOT EXISTS weather_live_1
    USING weather_live
    TAGS (1, '440500');

-- 3.2 气象子表：weather_live_2（电厂 ID=2）
CREATE TABLE IF NOT EXISTS weather_live_2
    USING weather_live
    TAGS (2, '440100');

-- 3.3 水文子表：hydro_level_1（水文站 ID=1，高要水文站）
CREATE TABLE IF NOT EXISTS hydro_level_1
    USING hydro_level
    TAGS (1);

-- 3.4 水文子表：hydro_level_2（水文站 ID=2，石角水文站）
CREATE TABLE IF NOT EXISTS hydro_level_2
    USING hydro_level
    TAGS (2);

-- ----------------------------------------------------------------------------
-- 4. 插入示例数据（单条写入 + 批量写入）
-- ----------------------------------------------------------------------------

-- 4.1 气象数据单条写入（指定子表名）
INSERT INTO weather_live_1 USING weather_live TAGS (1, '440500')
    VALUES (now, 28.5, 75.0, 5.2, 180, 0.0, 1013.2);

-- 4.2 气象数据批量写入（多行）
INSERT INTO weather_live_1 USING weather_live TAGS (1, '440500') VALUES
    (now - 10s, 28.4, 75.2, 5.0, 178, 0.0, 1013.3),
    (now - 20s, 28.3, 75.5, 4.8, 175, 0.0, 1013.4),
    (now - 30s, 28.2, 75.8, 4.5, 172, 0.0, 1013.5);

-- 4.3 水文数据写入
INSERT INTO hydro_level_1 USING hydro_level TAGS (1) VALUES
    (now,       8.52, 5200.0, false),
    (now - 1m,  8.50, 5180.0, false),
    (now - 2m,  8.48, 5150.0, false),
    (now - 3m,  8.45, 5120.0, false);

-- ----------------------------------------------------------------------------
-- 5. 常用查询示例
-- ----------------------------------------------------------------------------

-- 5.1 查询电厂 1 最近 1 小时的气象数据
SELECT ts, temp, humidity, wind_speed, wind_dir, rain_1h, pressure
FROM weather_live
WHERE plant_id = 1
  AND ts > now - 1h
ORDER BY ts DESC;

-- 5.2 查询某行政区划下所有电厂最新气象值（LAST_ROW 函数）
SELECT LAST_ROW(ts), LAST_ROW(temp), LAST_ROW(wind_speed), plant_id
FROM weather_live
WHERE district_code = '440100'
GROUP BY plant_id;

-- 5.3 水文站过去 24 小时水位趋势 + 是否超警戒
SELECT ts, water_level, flow, is_over_warning
FROM hydro_level
WHERE station_id = 1
  AND ts > now - 24h
ORDER BY ts ASC;

-- 5.4 过去 1 小时所有水文站最高水位（TAJOIN 降采样）
SELECT MAX(water_level), station_id
FROM hydro_level
WHERE ts > now - 1h
GROUP BY station_id;

-- 5.5 水位超警戒的站点实时列表（TWA / interpolation 降采样到 1 分钟）
SELECT LAST(water_level), station_id
FROM hydro_level
WHERE is_over_warning = true
  AND ts > now - 5m
GROUP BY station_id;

-- 5.6 电厂 1 温度 1 小时降采样（每 5 分钟平均值）
SELECT AVG(temp), AVG(humidity), AVG(wind_speed)
FROM weather_live
WHERE plant_id = 1
  AND ts > now - 1h
INTERVAL(5m);

-- ----------------------------------------------------------------------------
-- 6. 连续查询（Continuous Query）- 预聚合
--    每 5 分钟将气象数据聚合为 5 分钟级均值，写入分析库
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS energy_monitor_agg
    PRECISION 'ms' KEEP 3650 DURATION 30;

USE energy_monitor_agg;

-- 6.1 气象 5 分钟降采样表
CREATE STABLE IF NOT EXISTS weather_5m (
    ts            TIMESTAMP,
    avg_temp      FLOAT,
    max_temp      FLOAT,
    min_temp      FLOAT,
    avg_humidity  FLOAT,
    avg_wind_speed FLOAT,
    sum_rain_1h   FLOAT
)
TAGS (
    plant_id      BIGINT,
    district_code VARCHAR(12)
);

-- 6.2 连续查询：每 5 分钟执行一次，将原始数据降采样写入 weather_5m
CREATE CONTINUOUS QUERY IF NOT EXISTS cq_weather_5m
ON energy_monitor
RESAMPLE EVERY 5s
SELECT
    AVG(temp)        AS avg_temp,
    MAX(temp)        AS max_temp,
    MIN(temp)        AS min_temp,
    AVG(humidity)    AS avg_humidity,
    AVG(wind_speed)  AS avg_wind_speed,
    SUM(rain_1h)     AS sum_rain_1h
FROM weather_live
INTERVAL(5m)
GROUP BY plant_id, district_code
INTO energy_monitor_agg.weather_5m;

-- 6.3 水文 1 分钟降采样表
CREATE STABLE IF NOT EXISTS hydro_1m (
    ts               TIMESTAMP,
    avg_water_level  FLOAT,
    max_water_level  FLOAT,
    min_water_level  FLOAT,
    avg_flow         FLOAT
)
TAGS (
    station_id BIGINT
);

CREATE CONTINUOUS QUERY IF NOT EXISTS cq_hydro_1m
ON energy_monitor
RESAMPLE EVERY 5s
SELECT
    AVG(water_level) AS avg_water_level,
    MAX(water_level) AS max_water_level,
    MIN(water_level) AS min_water_level,
    AVG(flow)        AS avg_flow
FROM hydro_level
INTERVAL(1m)
GROUP BY station_id
INTO energy_monitor_agg.hydro_1m;

-- ----------------------------------------------------------------------------
-- 7. 索引策略说明
-- ----------------------------------------------------------------------------
-- TDengine 不需要显式创建索引，其查询性能优化机制：
-- 1. TAG 索引：所有 TAG 列自动建立内存索引，支持高效过滤（WHERE plant_id = 1）
-- 2. 时间分区：数据按时间分片存储，时间范围查询天然高效（WHERE ts > now - 1h）
-- 3. 列式存储：每列独立存储，查询指定列时 IO 放大极小
-- 4. 降采样：通过 INTERVAL() + CONTINUOUS QUERY 预聚合，加速长时间范围查询
-- 5. 缓存最新值：CACHELAST=1 缓存每个子表最后一条记录，LAST_ROW() 查询 O(1)
--
-- 性能基线（单节点参考）：
-- - 写入吞吐：> 100 万条/秒
-- - 单点查询延迟：< 10ms
-- - 聚合查询（1 天数据）：< 100ms

-- ============================================================================
-- 8. 超级表 prediction_result  预测结果数据
--    tags: target_id（水文站ID/电厂ID）, predict_type（预测类型）
--    columns: ts, forecast_value, upper_bound, lower_bound, confidence, is_fallback, model_used, generated_at
-- ============================================================================

USE energy_monitor;

CREATE STABLE IF NOT EXISTS prediction_result (
    ts              TIMESTAMP,   -- 预测目标时间戳
    forecast_value  FLOAT,       -- 预测值（水位 m / 发电量 MW）
    upper_bound     FLOAT,       -- 置信区间上界
    lower_bound     FLOAT,       -- 置信区间下界
    confidence      FLOAT,       -- 置信度（0-1）
    is_fallback     BOOL,        -- 是否为降级结果（true=本地ARIMA降级）
    model_used      VARCHAR(32), -- 实际使用的模型名称
    generated_at    VARCHAR(32)  -- 预测生成时间（yyyy-MM-dd HH:mm:ss）
)
TAGS (
    target_id      BIGINT,       -- 目标 ID（水文站 ID 或电厂 ID）
    predict_type   VARCHAR(24)   -- 预测类型：water_level / power_generation
)
COMMENT '预测结果超级表（每次预测生成一批时序行，按目标 + 类型分子表）';

ALTER STABLE prediction_result COMMENT COLUMN ts              '预测目标时间戳（该时间点的预测值）';
ALTER STABLE prediction_result COMMENT COLUMN forecast_value   '预测值（水位单位 m，发电量单位 MW）';
ALTER STABLE prediction_result COMMENT COLUMN upper_bound     '置信区间上界';
ALTER STABLE prediction_result COMMENT COLUMN lower_bound     '置信区间下界';
ALTER STABLE prediction_result COMMENT COLUMN confidence      '模型置信度（0-1，本地ARIMA降级约0.5）';
ALTER STABLE prediction_result COMMENT COLUMN is_fallback     '是否为降级结果（true=Python不可用，使用本地ARIMA）';
ALTER STABLE prediction_result COMMENT COLUMN model_used       '模型名称（lstm/xgboost/arima_local）';
ALTER STABLE prediction_result COMMENT COLUMN generated_at     '预测生成时间（yyyy-MM-dd HH:mm:ss）';
ALTER STABLE prediction_result COMMENT COLUMN target_id       '目标 ID（水文站或电厂）';
ALTER STABLE prediction_result COMMENT COLUMN predict_type    '预测类型：water_level / power_generation';

-- 8.1 子表示例（应用层写入时自动创建）
CREATE TABLE IF NOT EXISTS prediction_result_1
    USING prediction_result
    TAGS (1, 'water_level');

CREATE TABLE IF NOT EXISTS prediction_result_2
    USING prediction_result
    TAGS (2, 'water_level');

-- 8.2 查询示例：水文站 1 最近一次预测结果
SELECT ts, forecast_value, upper_bound, lower_bound, confidence, model_used
FROM prediction_result
WHERE target_id = 1
  AND predict_type = 'water_level'
ORDER BY ts DESC
LIMIT 48;

-- 8.3 查询降级预测占比
SELECT
    COUNT(*) AS total,
    SUM(CASE WHEN is_fallback = true THEN 1 ELSE 0 END) AS fallback_count
FROM prediction_result
WHERE target_id = 1
  AND ts > now - 24h;
