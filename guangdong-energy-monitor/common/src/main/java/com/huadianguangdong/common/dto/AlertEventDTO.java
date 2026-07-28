package com.huadianguangdong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报警事件 DTO
 * <p>
 * Drools 规则匹配成功后生成，用于持久化到 t_alert_record 并路由到推送服务。
 * 携带规则溯源信息（ruleId / ruleName / logicType）与聚合抑制信息（aggregationId）。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEventDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 规则 ID（t_alert_rule.id） */
    private Long ruleId;

    /** 规则名称 */
    private String ruleName;

    /** 规则逻辑类型：static_threshold / dynamic_deviation / combo_logic / trend_warning */
    private String logicType;

    /** 业务域类型：hydro / weather / fire / equipment / composite */
    private String ruleType;

    /** 电厂 ID */
    private Long plantId;

    /** 水文站 ID（水文规则适用） */
    private Long stationId;

    /** 行政区划代码 */
    private String districtCode;

    /** 报警级别：emergency / high / medium / low */
    private String level;

    /** 报警内容（描述触发原因） */
    private String content;

    /** 触发指标名（如 water_level / temp / wind_speed） */
    private String metric;

    /** 触发时指标值 */
    private Double metricValue;

    /** 阈值（静态阈值规则适用） */
    private Double threshold;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    /** 数据源时间戳（采集时间） */
    private LocalDateTime dataTime;

    /**
     * 聚合 ID（风暴抑制用）
     * <p>
     * 同区域 5 分钟内同类报警合并时，所有事件共享同一 aggregationId。
     * 格式：{ruleId}:{districtCode}:{5minBucket}
     */
    private String aggregationId;

    /** 是否被抑制（true 表示被风暴抑制/去重抑制，不单独推送） */
    private boolean suppressed;
}
