package com.huadianguangdong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 预测请求 DTO（通用）
 * <p>
 * 发送给 Python FastAPI 预测服务的统一请求载荷，
 * 支持水位预测、发电量预测等多场景。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 水文站 ID（水位预测场景） */
    private Long stationId;

    /** 电厂 ID（发电预测场景） */
    private Long plantId;

    /** 预测类型：water_level / power_generation */
    private String predictType;

    /** 历史数据序列（水位值序列 / 发电量序列） */
    private List<Double> historyData;

    /** 历史时间点序列（与 historyData 一一对应，yyyy-MM-dd HH:mm:ss） */
    private List<String> historyTimes;

    /** 预测未来时长（小时），默认 24h */
    private Integer forecastHours;

    /** 模型类型：lstm / xgboost / arima（指定使用哪种模型） */
    private String modelType;

    /**
     * 快捷构造水位预测请求
     */
    public static PredictionRequestDTO forWaterLevel(Long stationId,
                                                      List<Double> historyData,
                                                      List<String> historyTimes,
                                                      Integer forecastHours,
                                                      String modelType) {
        return PredictionRequestDTO.builder()
                .stationId(stationId)
                .predictType("water_level")
                .historyData(historyData)
                .historyTimes(historyTimes)
                .forecastHours(forecastHours)
                .modelType(modelType)
                .build();
    }

    /**
     * 快捷构造发电预测请求
     */
    public static PredictionRequestDTO forPower(Long plantId,
                                                List<Double> historyData,
                                                List<String> historyTimes,
                                                Integer forecastHours,
                                                String modelType) {
        return PredictionRequestDTO.builder()
                .plantId(plantId)
                .predictType("power_generation")
                .historyData(historyData)
                .historyTimes(historyTimes)
                .forecastHours(forecastHours)
                .modelType(modelType)
                .build();
    }
}
