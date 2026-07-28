package com.huadianguangdong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Kafka weather.raw 主题消息封装
 * <p>
 * 包含完整数据溯源链路：数据源标识 + 采集时间戳 + 原始值 + 清洗后值。
 * 下游消费者可据此做数据质量分析、回溯审计、异常值告警。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherRawMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 溯源字段 ====================

    /** 数据源标识：gd121（主源）/ cma（备用源） */
    private String source;

    /** 采集时间戳（ISO 8601，调度触发时间） */
    private String ts;

    /** 原始单条 JSON（未清洗，用于审计追溯） */
    private String raw;

    // ==================== 业务字段 ====================

    /** 电厂 ID */
    private Long plantId;

    /** 行政区划代码 */
    private String districtCode;

    // ==================== 清洗后值 ====================

    /** 清洗后的完整气象数据（异常值已标记 null） */
    private WeatherDataDTO cleaned;

    /**
     * 是否发生清洗（raw 与 cleaned 存在差异）
     */
    private boolean cleanedFlag;
}
