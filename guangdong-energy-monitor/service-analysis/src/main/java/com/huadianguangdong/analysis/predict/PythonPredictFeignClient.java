package com.huadianguangdong.analysis.predict;

import com.huadianguangdong.analysis.predict.fallback.PythonPredictFeignFallbackFactory;
import com.huadianguangdong.common.dto.PredictionRequestDTO;
import com.huadianguangdong.common.dto.PredictionResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Python 预测服务 Feign 客户端
 * <p>
 * 对接外部 Python FastAPI 预测服务，通过 {@code predict.python-api-url} 配置目标地址。
 * 集成降级工厂 {@link PythonPredictFeignFallbackFactory}，Python 服务不可用时自动切换本地 ARIMA 降级。
 *
 * @author huadianguangdong
 */
@FeignClient(
        name = "python-predict-service",
        url = "${predict.python-api-url}",
        fallbackFactory = PythonPredictFeignFallbackFactory.class
)
public interface PythonPredictFeignClient {

    /**
     * 水位预测：传入水文站历史水位数据，返回未来水位预测。
     *
     * @param request 预测请求（含 stationId / historyData / forecastHours / modelType）
     * @return 预测结果
     */
    @PostMapping("/api/predict/water-level")
    PredictionResponseDTO predictWaterLevel(@RequestBody PredictionRequestDTO request);

    /**
     * 发电预测：传入电厂气象历史，返回未来发电量预测。
     *
     * @param request 预测请求（含 plantId / historyData / forecastHours / modelType）
     * @return 预测结果
     */
    @PostMapping("/api/predict/power")
    PredictionResponseDTO predictPowerGeneration(@RequestBody PredictionRequestDTO request);
}
