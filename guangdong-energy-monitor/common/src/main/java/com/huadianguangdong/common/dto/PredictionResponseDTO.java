package com.huadianguangdong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 预测响应 DTO（通用）
 * <p>
 * Python FastAPI 预测服务返回的统一结果结构，
 * 支持水位预测、发电量预测等多场景。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预测类型：water_level / power_generation */
    private String predictType;

    /** 水文站 ID / 电厂 ID */
    private Long targetId;

    /** 预测的时间点序列（yyyy-MM-dd HH:mm:ss） */
    private List<String> forecastTimes;

    /** 预测值序列（与 forecastTimes 一一对应） */
    private List<Double> forecastValues;

    /** 上界序列（可选，置信区间上界） */
    private List<Double> upperBound;

    /** 下界序列（可选，置信区间下界） */
    private List<Double> lowerBound;

    /** 置信度（0-1） */
    private Double confidence;

    /** 实际使用的模型名称（lstm / xgboost / arima_local 等） */
    private String modelUsed;

    /** 是否为降级结果（true 表示 Python 服务不可用，使用了本地 ARIMA 降级） */
    private boolean fallback;

    /** 预测生成时间（yyyy-MM-dd HH:mm:ss） */
    private String generatedAt;

    /**
     * 获取预测最高值
     */
    public Double getMaxForecast() {
        if (forecastValues == null || forecastValues.isEmpty()) {
            return null;
        }
        return forecastValues.stream().max(Double::compareTo).orElse(null);
    }

    /**
     * 获取预测最低值
     */
    public Double getMinForecast() {
        if (forecastValues == null || forecastValues.isEmpty()) {
            return null;
        }
        return forecastValues.stream().min(Double::compareTo).orElse(null);
    }
}
