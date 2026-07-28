-- ============================================================================
-- PG 初始化脚本（PostGIS + 建表 + 初始数据）
-- 按数字前缀顺序执行
-- ============================================================================

-- PostGIS 扩展
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 枚举类型
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'power_plant_type') THEN
        CREATE TYPE power_plant_type AS ENUM ('coal', 'gas', 'solar', 'wind', 'storage');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'plant_status') THEN
        CREATE TYPE plant_status AS ENUM ('normal', 'warning', 'danger', 'offline');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'weather_warning_type') THEN
        CREATE TYPE weather_warning_type AS ENUM (
            'typhoon', 'rainstorm', 'high_temp', 'low_temp',
            'gale', 'fog', 'haze', 'thunder', 'ice', 'drought', 'flood', 'other'
        );
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'warning_level') THEN
        CREATE TYPE warning_level AS ENUM ('blue', 'yellow', 'orange', 'red');
    END IF;
END $$;

-- 电厂表
CREATE TABLE IF NOT EXISTS t_power_plant (
    id            bigserial       PRIMARY KEY,
    name          varchar(120)    NOT NULL,
    type          power_plant_type NOT NULL,
    location      geometry(Point, 4326) NOT NULL,
    district_code varchar(12)     NOT NULL,
    river_basin   varchar(60),
    capacity      numeric(10,2),
    status        plant_status    NOT NULL DEFAULT 'normal',
    address       varchar(255),
    create_time   timestamptz     NOT NULL DEFAULT now(),
    update_time   timestamptz     NOT NULL DEFAULT now(),
    is_deleted    smallint        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_power_plant_location ON t_power_plant USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_power_plant_type_status ON t_power_plant (type, status);

-- 水文站表
CREATE TABLE IF NOT EXISTS t_hydro_station (
    id              bigserial     PRIMARY KEY,
    name            varchar(120)  NOT NULL,
    river_basin     varchar(60)   NOT NULL,
    location        geometry(Point, 4326) NOT NULL,
    warning_level   numeric(6,2),
    guarantee_level numeric(6,2),
    historical_max  numeric(6,2),
    flow_capacity   numeric(10,2),
    city            varchar(50),
    status          plant_status  NOT NULL DEFAULT 'normal',
    create_time     timestamptz   NOT NULL DEFAULT now(),
    update_time     timestamptz   NOT NULL DEFAULT now(),
    is_deleted      smallint      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_hydro_station_location ON t_hydro_station USING GIST (location);

-- 气象预警表
CREATE TABLE IF NOT EXISTS t_weather_warning (
    id           bigserial            PRIMARY KEY,
    district_code varchar(12)         NOT NULL,
    type         weather_warning_type NOT NULL,
    level        warning_level        NOT NULL,
    publish_time timestamptz          NOT NULL,
    expire_time  timestamptz,
    content_json jsonb                NOT NULL,
    source       varchar(60),
    status       varchar(20)          NOT NULL DEFAULT 'active',
    create_time  timestamptz          NOT NULL DEFAULT now(),
    is_deleted   smallint             NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_warning_district_time ON t_weather_warning (district_code, publish_time);
CREATE INDEX IF NOT EXISTS idx_warning_type_level ON t_weather_warning (type, level);

-- 报警规则表
CREATE TABLE IF NOT EXISTS t_alert_rule (
    id            bigserial        PRIMARY KEY,
    name          varchar(120)     NOT NULL,
    rule_type     varchar(30)      NOT NULL,
    condition_json jsonb           NOT NULL,
    priority      smallint         NOT NULL DEFAULT 50,
    targets_json  jsonb,
    dead_zone     numeric(10,2),
    delay_sec     integer          NOT NULL DEFAULT 0,
    enabled       boolean          NOT NULL DEFAULT true,
    version       integer          NOT NULL DEFAULT 1,
    create_time   timestamptz      NOT NULL DEFAULT now(),
    update_time   timestamptz      NOT NULL DEFAULT now(),
    is_deleted    smallint         NOT NULL DEFAULT 0
);

-- update_time 触发器
CREATE OR REPLACE FUNCTION trg_set_update_time()
RETURNS trigger AS $$
BEGIN NEW.update_time := now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_plant_update BEFORE UPDATE ON t_power_plant FOR EACH ROW EXECUTE FUNCTION trg_set_update_time();
CREATE TRIGGER trg_hydro_update BEFORE UPDATE ON t_hydro_station FOR EACH ROW EXECUTE FUNCTION trg_set_update_time();

-- 初始电厂数据
INSERT INTO t_power_plant (name, type, location, district_code, river_basin, capacity, status, address) VALUES
('汕头华电发电有限公司',          'coal',   ST_SetSRID(ST_MakePoint(116.6821, 23.3535), 4326), '440500', '粤东沿海', 1200, 'normal', '广东省汕头市濠江区广澳港'),
('华电福新广州能源有限公司',      'gas',    ST_SetSRID(ST_MakePoint(113.4056, 23.0456), 4326), '440100', '珠江流域', 1200, 'normal', '广东省广州市南沙区黄阁镇'),
('广东华电清远能源有限公司',      'gas',    ST_SetSRID(ST_MakePoint(113.0624, 23.6821), 4326), '441800', '北江流域', 600,  'normal', '广东省清远市清城区石角镇'),
('广东华电惠州能源有限公司',      'gas',    ST_SetSRID(ST_MakePoint(114.4123, 23.1115), 4326), '441300', '东江流域', 900,  'normal', '广东省惠州市大亚湾石化区'),
('广东华电坪石发电有限公司',      'coal',   ST_SetSRID(ST_MakePoint(113.0521, 25.2856), 4326), '440200', '北江流域', 700,  'warning', '广东省韶关乐昌市坪石镇'),
('广东华电韶关热电有限公司',      'coal',   ST_SetSRID(ST_MakePoint(113.6024, 24.8123), 4326), '440200', '北江流域', 700,  'normal', '广东省韶关市浈江区乐园镇'),
('广东华电福新阳江海上风电',      'wind',   ST_SetSRID(ST_MakePoint(111.9821, 21.8635), 4326), '441700', '粤西沿海', 500,  'normal', '广东省阳江市阳西县溪头镇海域'),
('华电新能源集团广东分公司',      'solar',  ST_SetSRID(ST_MakePoint(113.2644, 23.1291), 4326), '440100', '珠江流域', 400,  'normal', '广东省广州市萝岗区'),
('广东华电深圳能源有限公司',      'gas',    ST_SetSRID(ST_MakePoint(114.0579, 22.5431), 4326), '440300', '珠江流域', 800,  'normal', '广东省深圳市宝安区');

-- 初始水文站数据
INSERT INTO t_hydro_station (name, river_basin, location, warning_level, guarantee_level, historical_max, flow_capacity, city) VALUES
('高要水文站',   '西江',  ST_SetSRID(ST_MakePoint(112.27, 23.05), 4326), 9.5, 13.0, 13.62, 50000, '肇庆'),
('石角水文站',   '北江',  ST_SetSRID(ST_MakePoint(112.96, 23.55), 4326), 11.0, 14.5, 15.36, 48000, '清远'),
('河源水文站',   '东江',  ST_SetSRID(ST_MakePoint(114.70, 23.74), 4326), 39.0, 42.0, 43.31, 12000, '河源'),
('博罗水文站',   '东江',  ST_SetSRID(ST_MakePoint(114.29, 23.17), 4326), 11.2, 13.0, 14.46, 15000, '惠州'),
('潮安水文站',   '韩江',  ST_SetSRID(ST_MakePoint(116.68, 23.46), 4326), 13.5, 15.5, 16.95, 13000, '潮州'),
('化州水文站',   '鉴江',  ST_SetSRID(ST_MakePoint(110.60, 21.66), 4326), 13.5, 15.8, 17.36, 9000,  '茂名');

-- 初始预警数据（为前端演示准备）
INSERT INTO t_weather_warning (district_code, type, level, publish_time, expire_time, content_json, source, status) VALUES
('440200', 'rainstorm', 'yellow', now() - interval '2 hours', now() + interval '10 hours',
 '{"title":"暴雨黄色预警","description":"韶关市预计未来6小时降雨量将达50mm以上","temperature":"28.5","humidity":"92"}', '广东省气象局', 'active'),
('440500', 'typhoon', 'orange', now() - interval '1 hour', now() + interval '24 hours',
 '{"title":"台风橙色预警","description":"台风"银河"预计今晚在汕头沿海登陆","temperature":"26.0","humidity":"95"}', '中央气象台', 'active'),
('440100', 'high_temp', 'red', now() - interval '3 hours', now() + interval '6 hours',
 '{"title":"高温红色预警","description":"广州市气温已达40℃，请做好防暑降温","temperature":"40.2","humidity":"45"}', '广州市气象局', 'active');

-- 初始报警规则
INSERT INTO t_alert_rule (name, rule_type, condition_json, priority, targets_json, dead_zone, delay_sec, enabled) VALUES
('北江石角站超警戒水位', 'hydro',
    '{"metric":"water_level","station_id":2,"op":">=","threshold":11.0,"duration":300}',
    20, '{"station_ids":[2],"districts":["441800"]}', 0.10, 300, true),
('台风红色预警全厂停机', 'weather',
    '{"metric":"weather_warning","type":"typhoon","level":"red","action":"shutdown"}',
    5,  '{"districts":["440100","440300","440500"]}', 0.00, 0, true);
