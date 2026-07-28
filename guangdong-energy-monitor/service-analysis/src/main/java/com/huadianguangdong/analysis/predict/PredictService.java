package com.huadianguangdong.analysis.predict;

import com.huadianguangdong.common.dto.PredictionResponseDTO;

/**
 * 预测服务接口
 *
 * <p>封装对 Python 预测服务的调用：组装历史数据 -> 调用 Feign -> 降级 -> 写入 TDengine -> 触发 TREND_WARNING。
 *
 * @author huadianguangdong
 */
public interface PredictService {

    /**
     * 水位预测（使用默认参数）
     *
     * @param stationId 水文站 ID
     * @return 预测结果
     */
    PredictionResponseDTO predictWaterLevel(Long stationId);

    /**
     * 水位预测（自定义预测时长和模型类型）
     *
     * @param stationId     水文站 ID
     * @param forecastHours 预测未来时长（小时）
     * @param modelType     模型类型（lstm / xgboost / arima）
     * @return 预测结果
     */
    PredictionResponseDTO predictWaterLevel(Long stationId, Integer forecastHours, String modelType);

    /**
     * 发电预测（使用默认参数）
     *
     * @param plantId 电厂 ID
     * @return 预测结果
     */
    PredictionResponseDTO predictPower(Long plantId);

    /**
     * 发电预测（自定义预测时长和模型类型）
     *
     * @param plantId       电厂 ID
     * @param forecastHours 预测未来时长（小时）
     * @param modelType     模型类型（lstm / xgboost / arima）
     * @return 预测结果
     */
    PredictionResponseDTO predictPower(Long plantId, Integer forecastHours, String modelType);
}
