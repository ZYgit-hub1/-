package com.huadianguangdong.analysis.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 电厂指标趋势 VO
 *
 * <p>用于折线图展示：按时间维度展示电厂某项指标（水位 / 功率等）的变化趋势。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantTrendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 电厂 ID */
    private Long plantId;

    /** 电厂名称 */
    private String plantName;

    /** 时间序列（yyyy-MM-dd HH:mm 或 yyyy-MM-dd） */
    private List<String> dates;

    /** 指标值序列（与 dates 一一对应） */
    private List<Double> values;

    /** 指标标识：waterLevel（水位）/ power（功率）等 */
    private String metric;
}
