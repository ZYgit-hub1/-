package com.huadianguangdong.analysis.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 预测结果 VO
 *
 * <p>统一承载水位预测 / 发电预测的返回结果。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预测类型：waterLevel / power */
    private String type;

    /** 预测的时间点序列 */
    private List<String> forecastTimes;

    /** 预测值序列（与 forecastTimes 一一对应） */
    private List<Double> forecastValues;

    /** 置信度（0-1） */
    private Double confidence;

    /** 模型名称 */
    private String model;

    /** 预测生成时间（yyyy-MM-dd HH:mm:ss） */
    private String generatedAt;
}
