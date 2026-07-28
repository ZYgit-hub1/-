-- ============================================================================
-- TDengine 初始化脚本
-- 自动按文件名顺序执行
-- ============================================================================

CREATE DATABASE IF NOT EXISTS energy_monitor;
USE energy_monitor;

-- ==================== 水文测报数据超级表 ====================
CREATE STABLE IF NOT EXISTS hydro_level (
    ts       TIMESTAMP,
    water_level FLOAT,        -- 水位（m）
    flow       FLOAT,          -- 流量（m³/s）
    rain_1h    FLOAT           -- 1小时降雨量（mm）
) TAGS (
    station_id BIGINT,
    station_name NCHAR(64)
);

-- 创建子表（对应水文站）
CREATE TABLE IF NOT EXISTS hydro_level_gaoyao USING hydro_level TAGS (1, '高要水文站');
CREATE TABLE IF NOT EXISTS hydro_level_shijiao USING hydro_level TAGS (2, '石角水文站');
CREATE TABLE IF NOT EXISTS hydro_level_heyuan USING hydro_level TAGS (3, '河源水文站');
CREATE TABLE IF NOT EXISTS hydro_level_boluo USING hydro_level TAGS (4, '博罗水文站');
CREATE TABLE IF NOT EXISTS hydro_level_chaoan USING hydro_level TAGS (5, '潮安水文站');
CREATE TABLE IF NOT EXISTS hydro_level_huazhou USING hydro_level TAGS (6, '化州水文站');

-- ==================== 气象实况数据超级表 ====================
CREATE STABLE IF NOT EXISTS weather_live (
    ts           TIMESTAMP,
    temperature  FLOAT,        -- 温度（℃）
    humidity     FLOAT,        -- 相对湿度（%）
    wind_speed   FLOAT,        -- 风速（m/s）
    wind_dir     SMALLINT,     -- 风向（度）
    pressure     FLOAT,        -- 气压（hPa）
    visibility   FLOAT         -- 能见度（km）
) TAGS (
    district_code NCHAR(12),
    city_name     NCHAR(32)
);

-- 创建部分子表
CREATE TABLE IF NOT EXISTS weather_live_gz USING weather_live TAGS ('440100', '广州');
CREATE TABLE IF NOT EXISTS weather_live_sg USING weather_live TAGS ('440200', '韶关');
CREATE TABLE IF NOT EXISTS weather_live_st USING weather_live TAGS ('440500', '汕头');
CREATE TABLE IF NOT EXISTS weather_live_hz USING weather_live TAGS ('441300', '惠州');
CREATE TABLE IF NOT EXISTS weather_live_yj USING weather_live TAGS ('441700', '阳江');
CREATE TABLE IF NOT EXISTS weather_live_sz USING weather_live TAGS ('440300', '深圳');
CREATE TABLE IF NOT EXISTS weather_live_qy USING weather_live TAGS ('441800', '清远');

-- ==================== 预测结果超级表 ====================
CREATE STABLE IF NOT EXISTS prediction_result (
    ts             TIMESTAMP,
    predict_value  FLOAT,        -- 预测值
    confidence     FLOAT,        -- 置信度（0-1）
    model_type     NCHAR(32)     -- 模型类型
) TAGS (
    target_id      BIGINT,
    predict_type   NCHAR(32)     -- 预测类型（water_level / power_output / temperature）
);

-- ==================== 电厂功率实况超级表 ====================
CREATE STABLE IF NOT EXISTS plant_power (
    ts             TIMESTAMP,
    active_power   FLOAT,        -- 有功功率（MW）
    load_rate      FLOAT         -- 负荷率（%）
) TAGS (
    plant_id       BIGINT,
    plant_name     NCHAR(64)
);

-- 创建部分子表（电厂）
CREATE TABLE IF NOT EXISTS plant_power_st USING plant_power TAGS (1, '汕头华电');
CREATE TABLE IF NOT EXISTS plant_power_gz USING plant_power TAGS (2, '广州华电');
CREATE TABLE IF NOT EXISTS plant_power_qy USING plant_power TAGS (3, '清远华电');
CREATE TABLE IF NOT EXISTS plant_power_hz USING plant_power TAGS (4, '惠州华电');
CREATE TABLE IF NOT EXISTS plant_power_ps USING plant_power TAGS (5, '坪石电厂');
CREATE TABLE IF NOT EXISTS plant_power_sg2 USING plant_power TAGS (6, '韶关热电');
CREATE TABLE IF NOT EXISTS plant_power_yj USING plant_power TAGS (7, '阳江风电');
CREATE TABLE IF NOT EXISTS plant_power_xn USING plant_power TAGS (8, '新能源广州');
CREATE TABLE IF NOT EXISTS plant_power_sz USING plant_power TAGS (9, '深圳华电');

-- ==================== 插入模拟历史数据（最近24小时，每5分钟一条） ====================

-- 水文站模拟数据
INSERT INTO hydro_level_shijiao
SELECT ts, 10.5 + sin(0.01 * (ts - '2026-07-27 00:00:00')) * 2.0 AS water_level,
       15000 + sin(0.008 * (ts - '2026-07-27 00:00:00')) * 5000 AS flow,
       0.0 AS rain_1h
FROM (SELECT _ts AS ts FROM (SELECT TIMESTAMPADD(MINUTE, ROWS, '2026-07-27 00:00:00') AS _ts
    FROM (SELECT ROW_NUMBER() OVER () AS ROWS FROM hydro_level_shijiao _t LIMIT 288) sub
    WHERE ROWS % 1 = 0) t WHERE _ts < '2026-07-27 23:55:00') time_seq;

INSERT INTO hydro_level_gaoyao
SELECT ts, 8.2 + sin(0.008 * (ts - '2026-07-27 00:00:00')) * 1.5 AS water_level,
       12000 + sin(0.006 * (ts - '2026-07-27 00:00:00')) * 4000 AS flow,
       0.5 AS rain_1h
FROM (SELECT _ts AS ts FROM (SELECT TIMESTAMPADD(MINUTE, ROWS, '2026-07-27 00:00:00') AS _ts
    FROM (SELECT ROW_NUMBER() OVER () AS ROWS FROM hydro_level_gaoyao _t LIMIT 288) sub
    WHERE ROWS % 1 = 0) t WHERE _ts < '2026-07-27 23:55:00');

INSERT INTO hydro_level_heyuan
SELECT ts, 37.0 + sin(0.012 * (ts - '2026-07-27 00:00:00')) * 3.0 AS water_level,
       8500 + sin(0.009 * (ts - '2026-07-27 00:00:00')) * 3000 AS flow,
       1.2 AS rain_1h
FROM (SELECT _ts AS ts FROM (SELECT TIMESTAMPADD(MINUTE, ROWS, '2026-07-27 00:00:00') AS _ts
    FROM (SELECT ROW_NUMBER() OVER () AS ROWS FROM hydro_level_heyuan _t LIMIT 288) sub
    WHERE ROWS % 1 = 0) t WHERE _ts < '2026-07-27 23:55:00');

-- 气象实况模拟数据
INSERT INTO weather_live_gz
SELECT ts,
       35 + sin(0.002 * (ts - '2026-07-27 00:00:00')) * 5.0 AS temperature,
       60 + sin(0.003 * (ts - '2026-07-27 00:00:00')) * 20.0 AS humidity,
       3.0 + sin(0.005 * (ts - '2026-07-27 00:00:00')) * 2.0 AS wind_speed,
       180 + CAST(sin(0.01 * (ts - '2026-07-27 00:00:00')) * 90 AS SMALLINT) AS wind_dir,
       1008.0 AS pressure,
       15.0 + sin(0.004 * (ts - '2026-07-27 00:00:00')) * 5.0 AS visibility
FROM (SELECT _ts AS ts FROM (SELECT TIMESTAMPADD(MINUTE, ROWS, '2026-07-27 00:00:00') AS _ts
    FROM (SELECT ROW_NUMBER() OVER () AS ROWS FROM weather_live_gz _t LIMIT 288) sub
    WHERE ROWS % 1 = 0) t WHERE _ts < '2026-07-27 23:55:00');

INSERT INTO weather_live_st
SELECT ts,
       28 + sin(0.002 * (ts - '2026-07-27 00:00:00')) * 3.0 AS temperature,
       85 + sin(0.003 * (ts - '2026-07-27 00:00:00')) * 10.0 AS humidity,
       8.0 + sin(0.005 * (ts - '2026-07-27 00:00:00')) * 5.0 AS wind_speed,
       90 + CAST(sin(0.01 * (ts - '2026-07-27 00:00:00')) * 45 AS SMALLINT) AS wind_dir,
       1005.0 AS pressure,
       8.0 + sin(0.004 * (ts - '2026-07-27 00:00:00')) * 3.0 AS visibility
FROM (SELECT _ts AS ts FROM (SELECT TIMESTAMPADD(MINUTE, ROWS, '2026-07-27 00:00:00') AS _ts
    FROM (SELECT ROW_NUMBER() OVER () AS ROWS FROM weather_live_st _t LIMIT 288) sub
    WHERE ROWS % 1 = 0) t WHERE _ts < '2026-07-27 23:55:00');

-- 电厂功率模拟数据
INSERT INTO plant_power_gz
SELECT ts,
       950 + sin(0.003 * (ts - '2026-07-27 00:00:00')) * 150 + rand() * 50 AS active_power,
       79.0 + sin(0.003 * (ts - '2026-07-27 00:00:00')) * 12.0 + rand() * 5.0 AS load_rate
FROM (SELECT _ts AS ts FROM (SELECT TIMESTAMPADD(MINUTE, ROWS, '2026-07-27 00:00:00') AS _ts
    FROM (SELECT ROW_NUMBER() OVER () AS ROWS FROM plant_power_gz _t LIMIT 288) sub
    WHERE ROWS % 1 = 0) t WHERE _ts < '2026-07-27 23:55:00');

INSERT INTO plant_power_st
SELECT ts,
       1050 + sin(0.004 * (ts - '2026-07-27 00:00:00')) * 100 + rand() * 30 AS active_power,
       87.0 + sin(0.004 * (ts - '2026-07-27 00:00:00')) * 8.0 + rand() * 3.0 AS load_rate
FROM (SELECT _ts AS ts FROM (SELECT TIMESTAMPADD(MINUTE, ROWS, '2026-07-27 00:00:00') AS _ts
    FROM (SELECT ROW_NUMBER() OVER () AS ROWS FROM plant_power_st _t LIMIT 288) sub
    WHERE ROWS % 1 = 0) t WHERE _ts < '2026-07-27 23:55:00');

INSERT INTO plant_power_sz
SELECT ts,
       650 + sin(0.003 * (ts - '2026-07-27 00:00:00')) * 120 + rand() * 40 AS active_power,
       81.0 + sin(0.003 * (ts - '2026-07-27 00:00:00')) * 15.0 + rand() * 4.0 AS load_rate
FROM (SELECT _ts AS ts FROM (SELECT TIMESTAMPADD(MINUTE, ROWS, '2026-07-27 00:00:00') AS _ts
    FROM (SELECT ROW_NUMBER() OVER () AS ROWS FROM plant_power_sz _t LIMIT 288) sub
    WHERE ROWS % 1 = 0) t WHERE _ts < '2026-07-27 23:55:00');

INSERT INTO plant_power_yj
SELECT ts,
       280 + sin(0.005 * (ts - '2026-07-27 00:00:00')) * 180 + rand() * 20 AS active_power,
       56.0 + sin(0.005 * (ts - '2026-07-27 00:00:00')) * 36.0 + rand() * 4.0 AS load_rate
FROM (SELECT _ts AS ts FROM (SELECT TIMESTAMPADD(MINUTE, ROWS, '2026-07-27 00:00:00') AS _ts
    FROM (SELECT ROW_NUMBER() OVER () AS ROWS FROM plant_power_yj _t LIMIT 288) sub
    WHERE ROWS % 1 = 0) t WHERE _ts < '2026-07-27 23:55:00');
