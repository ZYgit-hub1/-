-- ============================================================================
-- 广东省电厂监控平台 - PostgreSQL 14+ / PostGIS 3.x DDL
-- 业务关系数据 + 空间数据
-- 字符集：UTF8    排序规则：zh_CN.UTF-8 或 C.UTF-8
-- 主键策略：雪花 ID（ bigint ），由应用层生成，DB 层使用 bigserial 兜底
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. 扩展安装（需 superuser 权限，每个集群执行一次）
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
CREATE EXTENSION IF NOT EXISTS btree_gist;     -- 支持时间+空间联合 GiST 索引
CREATE EXTENSION IF NOT EXISTS pg_trgm;        -- 模糊查询（名称 LIKE '%xxx%'）

-- ----------------------------------------------------------------------------
-- 0.1 枚举类型定义
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    -- 电厂类型
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'power_plant_type') THEN
        CREATE TYPE power_plant_type AS ENUM ('coal', 'gas', 'solar', 'wind', 'storage');
    END IF;

    -- 运行状态
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'plant_status') THEN
        CREATE TYPE plant_status AS ENUM ('normal', 'warning', 'danger', 'offline');
    END IF;

    -- 气象预警类型
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'weather_warning_type') THEN
        CREATE TYPE weather_warning_type AS ENUM (
            'typhoon',    'rainstorm',  'high_temp',  'low_temp',
            'gale',       'fog',        'haze',       'thunder',
            'ice',        'drought',    'flood',      'other'
        );
    END IF;

    -- 预警级别（对应国家突发气象灾害预警信号）
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'warning_level') THEN
        CREATE TYPE warning_level AS ENUM ('blue', 'yellow', 'orange', 'red');
    END IF;

    -- 规则类型
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'alert_rule_type') THEN
        CREATE TYPE alert_rule_type AS ENUM ('hydro', 'weather', 'fire', 'equipment', 'composite');
    END IF;
END $$;

COMMENT ON TYPE power_plant_type     IS '电厂类型：coal燃煤 / gas燃气 / solar光伏 / wind风电 / storage储能';
COMMENT ON TYPE plant_status         IS '电厂运行状态：normal正常 / warning预警 / danger报警 / offline离线';
COMMENT ON TYPE weather_warning_type IS '气象预警类型：typhoon台风 / rainstorm暴雨 / high_temp高温 / gale大风 / fog大雾 / haze霾 / thunder雷暴 / ice结冰 / drought干旱 / flood洪水 / other其他';
COMMENT ON TYPE warning_level        IS '预警级别：blue蓝 / yellow黄 / orange橙 / red红';
COMMENT ON TYPE alert_rule_type      IS '规则类型：hydro水文 / weather气象 / fire火情 / equipment设备 / composite组合';

-- ----------------------------------------------------------------------------
-- 1. t_power_plant  电厂表（含 PostGIS 空间列）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_power_plant CASCADE;
CREATE TABLE t_power_plant (
    id            bigserial       PRIMARY KEY,
    name          varchar(120)    NOT NULL,
    type          power_plant_type NOT NULL,
    location      geometry(Point, 4326) NOT NULL,          -- WGS84 经纬度
    district_code varchar(12)     NOT NULL,                 -- 行政区划代码（民政部 6/12 位）
    river_basin   varchar(60),                              -- 所属流域（珠江/韩江/粤东沿海/粤西沿海）
    capacity      numeric(10,2),                            -- 装机容量（MW）
    status        plant_status    NOT NULL DEFAULT 'normal',
    address       varchar(255),
    create_time   timestamptz     NOT NULL DEFAULT now(),
    update_time   timestamptz     NOT NULL DEFAULT now(),
    is_deleted    smallint        NOT NULL DEFAULT 0
);

-- 空间索引（GiST）—— 支持 ST_DWithin / ST_Contains 等空间查询
CREATE INDEX idx_power_plant_location ON t_power_plant USING GIST (location);
-- 唯一约束：电厂名称
CREATE UNIQUE INDEX uk_power_plant_name ON t_power_plant (name) WHERE is_deleted = 0;
-- 复合索引：按类型+状态筛选（大屏分类统计）
CREATE INDEX idx_plant_type_status    ON t_power_plant (type, status);
-- 行政区划索引（按地市聚合）
CREATE INDEX idx_plant_district       ON t_power_plant (district_code);
-- 流域索引（水文关联分析）
CREATE INDEX idx_plant_river_basin    ON t_power_plant (river_basin);

COMMENT ON TABLE  t_power_plant IS '电厂基础信息表（含 PostGIS 空间位置）';
COMMENT ON COLUMN t_power_plant.id            IS '主键 ID（雪花 ID）';
COMMENT ON COLUMN t_power_plant.name          IS '电厂名称（唯一）';
COMMENT ON COLUMN t_power_plant.type          IS '电厂类型：coal/gas/solar/wind/storage';
COMMENT ON COLUMN t_power_plant.location      IS 'WGS84 经纬度点，SRID=4326，示例：SRID=4326;POINT(113.2644 23.1291)';
COMMENT ON COLUMN t_power_plant.district_code IS '行政区划代码，如 440100（广州市），支持省/市/区县三级';
COMMENT ON COLUMN t_power_plant.river_basin   IS '所属流域：珠江流域/韩江流域/粤东沿海/粤西沿海/粤北内陆';
COMMENT ON COLUMN t_power_plant.capacity      IS '装机容量（MW）';
COMMENT ON COLUMN t_power_plant.status        IS '运行状态：normal/warning/danger/offline';
COMMENT ON COLUMN t_power_plant.address       IS '详细地址';
COMMENT ON COLUMN t_power_plant.create_time   IS '创建时间（带时区）';
COMMENT ON COLUMN t_power_plant.update_time   IS '更新时间（带时区）';
COMMENT ON COLUMN t_power_plant.is_deleted    IS '逻辑删除：0未删除 / 1已删除';

-- 更新时间触发器
CREATE OR REPLACE FUNCTION trg_set_update_time()
RETURNS trigger AS $$
BEGIN
    NEW.update_time := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_power_plant_update_time
    BEFORE UPDATE ON t_power_plant
    FOR EACH ROW EXECUTE FUNCTION trg_set_update_time();

-- ----------------------------------------------------------------------------
-- 2. t_hydro_station  水文站表（含 PostGIS 空间列）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_hydro_station CASCADE;
CREATE TABLE t_hydro_station (
    id              bigserial     PRIMARY KEY,
    name            varchar(120)  NOT NULL,
    river_basin     varchar(60)   NOT NULL,                  -- 所属河流流域
    location        geometry(Point, 4326) NOT NULL,
    warning_level   numeric(6,2),                            -- 警戒水位（m）
    guarantee_level numeric(6,2),                            -- 保证水位（m）
    historical_max  numeric(6,2),                            -- 历史最高水位（m）
    flow_capacity   numeric(10,2),                           -- 测流能力（m³/s）
    city            varchar(50),
    status          plant_status  NOT NULL DEFAULT 'normal',
    create_time     timestamptz   NOT NULL DEFAULT now(),
    update_time     timestamptz   NOT NULL DEFAULT now(),
    is_deleted      smallint      NOT NULL DEFAULT 0
);

CREATE INDEX idx_hydro_station_location ON t_hydro_station USING GIST (location);
CREATE UNIQUE INDEX uk_hydro_station_name ON t_hydro_station (name) WHERE is_deleted = 0;
CREATE INDEX idx_hydro_station_river   ON t_hydro_station (river_basin);
CREATE INDEX idx_hydro_station_city    ON t_hydro_station (city);
CREATE INDEX idx_hydro_station_status  ON t_hydro_station (status);

COMMENT ON TABLE  t_hydro_station IS '水文站基础信息表';
COMMENT ON COLUMN t_hydro_station.id               IS '主键 ID';
COMMENT ON COLUMN t_hydro_station.name             IS '水文站名称（唯一）';
COMMENT ON COLUMN t_hydro_station.river_basin      IS '所属河流/流域：西江/北江/东江/韩江/鉴江/珠江三角洲';
COMMENT ON COLUMN t_hydro_station.location         IS 'WGS84 经纬度，SRID=4326';
COMMENT ON COLUMN t_hydro_station.warning_level    IS '警戒水位（m），超过即触发预警';
COMMENT ON COLUMN t_hydro_station.guarantee_level  IS '保证水位（m），超过即防汛应急响应';
COMMENT ON COLUMN t_hydro_station.historical_max   IS '历史最高水位（m），用于风险评估';
COMMENT ON COLUMN t_hydro_station.flow_capacity    IS '测流能力（m³/s），最大可测流量';
COMMENT ON COLUMN t_hydro_station.city             IS '所在城市';
COMMENT ON COLUMN t_hydro_station.status           IS '运行状态：normal/warning/danger/offline';

CREATE TRIGGER trg_hydro_station_update_time
    BEFORE UPDATE ON t_hydro_station
    FOR EACH ROW EXECUTE FUNCTION trg_set_update_time();

-- ----------------------------------------------------------------------------
-- 3. t_weather_warning  气象预警表（区县级分区表）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_weather_warning CASCADE;
CREATE TABLE t_weather_warning (
    id           bigserial            PRIMARY KEY,
    district_code varchar(12)         NOT NULL,
    type         weather_warning_type NOT NULL,
    level        warning_level        NOT NULL,
    publish_time timestamptz          NOT NULL,            -- 预警发布时间
    expire_time  timestamptz,                              -- 预警失效时间（NULL 表示长期有效）
    content_json jsonb                NOT NULL,            -- 预警详情（标题/描述/影响区域/防御指南）
    source       varchar(60),                              -- 数据来源（CMA/省气象局/市气象局）
    status       varchar(20)          NOT NULL DEFAULT 'active',  -- active生效/expired过期/cancelled撤销
    create_time  timestamptz          NOT NULL DEFAULT now(),
    is_deleted   smallint             NOT NULL DEFAULT 0
) PARTITION BY RANGE (publish_time);

-- 分区策略：按月分区（示例创建 2026 年 7 月、8 月分区，后续由 pg_partman 自动维护）
CREATE TABLE t_weather_warning_202607 PARTITION OF t_weather_warning
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE t_weather_warning_202608 PARTITION OF t_weather_warning
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

-- 索引（在每个分区上自动创建）
CREATE INDEX idx_warning_district_time ON t_weather_warning (district_code, publish_time);
CREATE INDEX idx_warning_type_level    ON t_weather_warning (type, level);
CREATE INDEX idx_warning_expire_time   ON t_weather_warning (expire_time) WHERE status = 'active';
-- GIN 索引支持 jsonb 内容检索
CREATE INDEX idx_warning_content_gin   ON t_weather_warning USING GIN (content_json);

COMMENT ON TABLE  t_weather_warning IS '气象预警表（按月分区，存储 CMA/省气象局发布的预警信号）';
COMMENT ON COLUMN t_weather_warning.id            IS '主键 ID';
COMMENT ON COLUMN t_weather_warning.district_code IS '受影响行政区划代码';
COMMENT ON COLUMN t_weather_warning.type          IS '预警类型：typhoon/rainstorm/high_temp/gale/fog/haze/thunder/ice/drought/flood/other';
COMMENT ON COLUMN t_weather_warning.level         IS '预警级别：blue蓝/yellow黄/orange橙/red红';
COMMENT ON COLUMN t_weather_warning.publish_time  IS '预警发布时间（分区键，按月分区）';
COMMENT ON COLUMN t_weather_warning.expire_time   IS '预警失效时间，NULL 表示长期有效';
COMMENT ON COLUMN t_weather_warning.content_json  IS '预警详情 JSONB：{title,description,areas:[...],defense:[...]}';
COMMENT ON COLUMN t_weather_warning.source        IS '预警来源：CMA中央气象台/省气象局/市气象局';
COMMENT ON COLUMN t_weather_warning.status        IS '状态：active生效中 / expired已过期 / cancelled已撤销';

-- ----------------------------------------------------------------------------
-- 4. t_alert_rule  报警规则表（Drools 规则元数据）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_alert_rule CASCADE;
CREATE TABLE t_alert_rule (
    id            bigserial        PRIMARY KEY,
    name          varchar(120)     NOT NULL,
    rule_type     alert_rule_type  NOT NULL,
    condition_json jsonb           NOT NULL,                -- 规则条件（JSON 表达式，供 Drools 动态加载）
    priority      smallint         NOT NULL DEFAULT 50,     -- 优先级（1-100，数字越小优先级越高）
    targets_json  jsonb,                                   -- 规则作用目标 {plant_ids:[], station_ids:[], districts:[]}
    dead_zone     numeric(10,2),                            -- 死区（避免频繁抖动，如水位 ±0.1m）
    delay_sec     integer          NOT NULL DEFAULT 0,      -- 延迟触发秒数（防止瞬时尖峰误报）
    enabled       boolean          NOT NULL DEFAULT true,
    version       integer          NOT NULL DEFAULT 1,      -- 乐观锁版本号
    create_time   timestamptz      NOT NULL DEFAULT now(),
    update_time   timestamptz      NOT NULL DEFAULT now(),
    is_deleted    smallint         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_alert_rule_name     ON t_alert_rule (name) WHERE is_deleted = 0;
CREATE INDEX idx_alert_rule_type_enabled   ON t_alert_rule (rule_type, enabled);
CREATE INDEX idx_alert_rule_priority       ON t_alert_rule (priority);
CREATE INDEX idx_alert_rule_condition_gin  ON t_alert_rule USING GIN (condition_json);
CREATE INDEX idx_alert_rule_targets_gin    ON t_alert_rule USING GIN (targets_json);

COMMENT ON TABLE  t_alert_rule IS '报警规则配置表（驱动 Drools 规则引擎动态加载）';
COMMENT ON COLUMN t_alert_rule.id            IS '主键 ID';
COMMENT ON COLUMN t_alert_rule.name          IS '规则名称（唯一）';
COMMENT ON COLUMN t_alert_rule.rule_type     IS '规则类型：hydro/weather/fire/equipment/composite';
COMMENT ON COLUMN t_alert_rule.condition_json IS '规则条件 JSONB，示例：{"metric":"water_level","op":">=","threshold":13.0,"duration":300}';
COMMENT ON COLUMN t_alert_rule.priority      IS '优先级 1-100，数字越小优先级越高，同级按触发时间排序';
COMMENT ON COLUMN t_alert_rule.targets_json  IS '作用目标 JSONB：{"plant_ids":[1,2],"station_ids":[3,4],"districts":["440100"]}';
COMMENT ON COLUMN t_alert_rule.dead_zone     IS '死区阈值（避免频繁抖动），如水位 ±0.1m 内不重复报警';
COMMENT ON COLUMN t_alert_rule.delay_sec     IS '延迟触发秒数，条件持续满足 N 秒后才生成报警';
COMMENT ON COLUMN t_alert_rule.enabled       IS '是否启用：true启用 / false禁用';
COMMENT ON COLUMN t_alert_rule.version       IS '乐观锁版本号，每次更新+1';

CREATE TRIGGER trg_alert_rule_update_time
    BEFORE UPDATE ON t_alert_rule
    FOR EACH ROW EXECUTE FUNCTION trg_set_update_time();

-- ----------------------------------------------------------------------------
-- 5. 常用空间查询函数
-- ----------------------------------------------------------------------------

-- 5.1 根据经纬度查询半径 R 米内的电厂
CREATE OR REPLACE FUNCTION find_plants_within_radius(
    p_lng double precision,
    p_lat double precision,
    p_radius_m integer
)
RETURNS TABLE (
    id bigint,
    name varchar,
    type power_plant_type,
    status plant_status,
    distance_m double precision
)
LANGUAGE sql STABLE
AS $$
    SELECT
        t.id,
        t.name,
        t.type,
        t.status,
        ST_DistanceSphere(t.location, ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)) AS distance_m
    FROM t_power_plant t
    WHERE t.is_deleted = 0
      AND ST_DWithin(
            t.location::geography,
            ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography,
            p_radius_m
          )
    ORDER BY distance_m ASC;
$$;

COMMENT ON FUNCTION find_plants_within_radius IS '查询指定经纬度半径 R 米内的电厂（按距离升序）';

-- 5.2 查询某行政区划边界内的电厂（需预先导入行政区 polygon）
-- 示例：SELECT * FROM find_plants_in_district('440100');
CREATE OR REPLACE FUNCTION find_plants_in_district(p_district_code varchar)
RETURNS TABLE (
    id bigint,
    name varchar,
    type power_plant_type,
    status plant_status
)
LANGUAGE sql STABLE
AS $$
    SELECT t.id, t.name, t.type, t.status
    FROM t_power_plant t
    WHERE t.is_deleted = 0
      AND t.district_code = p_district_code
    ORDER BY t.name;
$$;

-- ----------------------------------------------------------------------------
-- 6. 初始化数据（华电广东电厂真实数据示例）
-- ----------------------------------------------------------------------------
INSERT INTO t_power_plant (name, type, location, district_code, river_basin, capacity, status, address) VALUES
('汕头华电发电有限公司',          'coal',   ST_SetSRID(ST_MakePoint(116.6821, 23.3535), 4326), '440500', '粤东沿海', 1200, 'normal', '广东省汕头市濠江区广澳港'),
('华电福新广州能源有限公司',      'gas',    ST_SetSRID(ST_MakePoint(113.4056, 23.0456), 4326), '440100', '珠江流域', 1200, 'normal', '广东省广州市南沙区黄阁镇'),
('广东华电清远能源有限公司',      'gas',    ST_SetSRID(ST_MakePoint(113.0624, 23.6821), 4326), '441800', '北江流域', 600,  'normal', '广东省清远市清城区石角镇'),
('广东华电惠州能源有限公司',      'gas',    ST_SetSRID(ST_MakePoint(114.4123, 23.1115), 4326), '441300', '东江流域', 900,  'normal', '广东省惠州市大亚湾石化区'),
('广东华电坪石发电有限公司',      'coal',   ST_SetSRID(ST_MakePoint(113.0521, 25.2856), 4326), '440200', '北江流域', 700,  'normal', '广东省韶关乐昌市坪石镇'),
('广东华电韶关热电有限公司',      'coal',   ST_SetSRID(ST_MakePoint(113.6024, 24.8123), 4326), '440200', '北江流域', 700,  'normal', '广东省韶关市浈江区乐园镇'),
('广东华电福新阳江海上风电',      'wind',   ST_SetSRID(ST_MakePoint(111.9821, 21.8635), 4326), '441700', '粤西沿海', 500,  'normal', '广东省阳江市阳西县溪头镇海域'),
('华电新能源集团广东分公司',      'solar',  ST_SetSRID(ST_MakePoint(113.2644, 23.1291), 4326), '440100', '珠江流域', 400,  'normal', '广东省广州市萝岗区开发区科汇四街3号'),
('广东华电深圳能源有限公司',      'gas',    ST_SetSRID(ST_MakePoint(114.0579, 22.5431), 4326), '440300', '珠江流域', 800,  'normal', '广东省深圳市宝安区福永街道');

INSERT INTO t_hydro_station (name, river_basin, location, warning_level, guarantee_level, historical_max, flow_capacity, city) VALUES
('高要水文站',   '西江',     ST_SetSRID(ST_MakePoint(112.27, 23.05), 4326), 9.5,  13.0, 13.62, 50000, '肇庆'),
('石角水文站',   '北江',     ST_SetSRID(ST_MakePoint(112.96, 23.55), 4326), 11.0, 14.5, 15.36, 48000, '清远'),
('河源水文站',   '东江',     ST_SetSRID(ST_MakePoint(114.70, 23.74), 4326), 39.0, 42.0, 43.31, 12000, '河源'),
('博罗水文站',   '东江',     ST_SetSRID(ST_MakePoint(114.29, 23.17), 4326), 11.2, 13.0, 14.46, 15000, '惠州'),
('潮安水文站',   '韩江',     ST_SetSRID(ST_MakePoint(116.68, 23.46), 4326), 13.5, 15.5, 16.95, 13000, '潮州'),
('化州水文站',   '鉴江',     ST_SetSRID(ST_MakePoint(110.60, 21.66), 4326), 13.5, 15.8, 17.36, 9000,  '茂名');

INSERT INTO t_alert_rule (name, rule_type, condition_json, priority, targets_json, dead_zone, delay_sec, enabled) VALUES
('北江石角站超警戒水位', 'hydro',
    '{"metric":"water_level","station_id":2,"op":">=","threshold":11.0,"duration":300}',
    20, '{"station_ids":[2],"districts":["441800"]}', 0.10, 300, true),
('台风红色预警全厂停机', 'weather',
    '{"metric":"weather_warning","type":"typhoon","level":"red","action":"shutdown"}',
    5,  '{"districts":["440100","440300","440500"]}', 0.00, 0, true),
('风电场风速超限保护', 'equipment',
    '{"metric":"wind_speed","op":">","threshold":25.0,"plant_types":["wind"],"duration":600}',
    30, '{"plant_types":["wind"]}', 1.0, 600, true);
