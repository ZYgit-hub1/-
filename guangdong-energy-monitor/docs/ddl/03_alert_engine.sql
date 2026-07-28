-- ============================================================================
-- 报警规则引擎扩展 DDL
-- 补充 t_alert_record 表、rule_logic_type 类型、t_alert_rule.logic_type 列
-- ============================================================================

-- ==================== 1. 新增规则逻辑类型枚举 ====================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'rule_logic_type') THEN
        CREATE TYPE rule_logic_type AS ENUM (
            'static_threshold',
            'dynamic_deviation',
            'combo_logic',
            'trend_warning'
        );
    END IF;
END $$;

COMMENT ON TYPE rule_logic_type IS '规则逻辑类型：static_threshold静态阈值 / dynamic_deviation动态偏离 / combo_logic组合逻辑 / trend_warning趋势预警';

-- ==================== 2. t_alert_rule 增加 logic_type 列 ====================
ALTER TABLE t_alert_rule
    ADD COLUMN IF NOT EXISTS logic_type rule_logic_type NOT NULL DEFAULT 'static_threshold';

COMMENT ON COLUMN t_alert_rule.logic_type IS '规则逻辑类型（决定 Drools 匹配模式），与 rule_type（业务域）正交';

-- ==================== 3. t_alert_record 报警记录表 ====================
DROP TABLE IF EXISTS t_alert_record CASCADE;
CREATE TABLE t_alert_record (
    id              bigserial       PRIMARY KEY,
    rule_id         bigint          NOT NULL,                    -- 规则 ID（关联 t_alert_rule.id）
    rule_name       varchar(120)    NOT NULL,                    -- 规则名称（冗余）
    logic_type      varchar(30)     NOT NULL,                    -- 规则逻辑类型
    rule_type       varchar(20)     NOT NULL,                    -- 业务域类型
    plant_id        bigint,                                       -- 电厂 ID
    station_id      bigint,                                       -- 水文站 ID
    district_code   varchar(10),                                  -- 行政区划代码
    level           varchar(15)     NOT NULL,                    -- 报警级别：emergency/high/medium/low
    content         text            NOT NULL,                    -- 报警内容
    metric          varchar(50),                                  -- 触发指标名
    metric_value    double precision,                             -- 触发时指标值
    threshold       double precision,                             -- 阈值
    trigger_time    timestamptz     NOT NULL,                    -- 触发时间
    data_time       timestamptz,                                  -- 数据源时间戳
    aggregation_id  varchar(80),                                  -- 聚合 ID（风暴抑制用）
    suppressed      boolean         NOT NULL DEFAULT false,      -- 是否被抑制
    push_status     varchar(15)     NOT NULL DEFAULT 'pending',  -- 推送状态：pending/pushed/suppressed/failed
    create_time     timestamptz     NOT NULL DEFAULT now(),
    is_deleted      smallint        NOT NULL DEFAULT 0
);

-- 索引
CREATE INDEX idx_alert_record_rule_id      ON t_alert_record (rule_id);
CREATE INDEX idx_alert_record_plant_id     ON t_alert_record (plant_id);
CREATE INDEX idx_alert_record_trigger_time ON t_alert_record (trigger_time);
CREATE INDEX idx_alert_record_agg_id       ON t_alert_record (aggregation_id);
CREATE INDEX idx_alert_record_level_time   ON t_alert_record (level, trigger_time);

COMMENT ON TABLE  t_alert_record IS '报警记录表（Drools 规则匹配 + 抑制后持久化）';
COMMENT ON COLUMN t_alert_record.rule_id        IS '规则 ID（关联 t_alert_rule.id）';
COMMENT ON COLUMN t_alert_record.logic_type     IS '规则逻辑类型：static_threshold/dynamic_deviation/combo_logic/trend_warning';
COMMENT ON COLUMN t_alert_record.aggregation_id IS '聚合 ID（风暴抑制：同区域5分钟内同类报警合并）';
COMMENT ON COLUMN t_alert_record.suppressed     IS '是否被抑制（true 表示被合并/去重，不单独推送）';
COMMENT ON COLUMN t_alert_record.push_status    IS '推送状态：pending待推送/pushed已推送/suppressed已抑制/failed推送失败';

-- ==================== 4. 插入示例规则（含 logic_type） ====================
INSERT INTO t_alert_rule (name, rule_type, logic_type, condition_json, priority, targets_json, dead_zone, delay_sec, enabled) VALUES
(
    '暴雨预警-静态阈值',
    'weather',
    'static_threshold',
    '{"metric":"rain1h","op":">","threshold":50.0}'::jsonb,
    40,
    '{"plant_ids":[1,2,3,4,5,6,7,8,9]}'::jsonb,
    5.0,
    300,
    true
),
(
    '火灾风险-组合逻辑',
    'weather',
    'combo_logic',
    '{"conditions":[{"metric":"temp","op":">","threshold":35.0},{"metric":"humidity","op":"<","threshold":30.0},{"metric":"windSpeed","op":">","threshold":10.0}],"logic":"AND"}'::jsonb,
    50,
    '{"plant_ids":[1,2,3,4,5,6,7,8,9]}'::jsonb,
    null,
    0,
    true
),
(
    '超保证水位-静态阈值',
    'hydro',
    'static_threshold',
    '{"metric":"water_level","op":">","threshold":15.0}'::jsonb,
    50,
    null,
    0.1,
    60,
    true
)
ON CONFLICT (name) DO NOTHING;
