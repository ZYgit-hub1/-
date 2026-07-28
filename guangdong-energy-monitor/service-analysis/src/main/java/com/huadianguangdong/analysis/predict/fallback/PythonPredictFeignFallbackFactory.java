package com.huadianguangdong.analysis.predict.fallback;

import com.huadianguangdong.analysis.predict.LocalArimaPredictor;
import com.huadianguangdong.analysis.predict.PythonPredictFeignClient;
import com.huadianguangdong.common.dto.PredictionRequestDTO;
import com.huadianguangdong.common.dto.PredictionResponseDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Python 预测服务 Feign 降级工厂
 * <p>
 * 当 Python FastAPI 服务超时、不可用或返回异常时，
 * 自动切换到本地 {@link LocalArimaPredictor}（轻量 ARIMA 实现）作为降级方案。
 *
 * @author huadianguangdong
 */
@Slf4j
public class PythonPredictFeignFallbackFactory implements FallbackFactory<PythonPredictFeignClient> {

    private final LocalArimaPredictor localArimaPredictor;

    public PythonPredictFeignFallbackFactory(LocalArimaPredictor localArimaPredictor) {
        this.localArimaPredictor = localArimaPredictor;
    }

    @Override
    public PythonPredictFeignClient create(Throwable cause) {
        log.warn("[预测降级] Python 服务不可用，切换本地 ARIMA 降级: {}", cause.getMessage());
        return new PythonPredictFeignClient() {
            @Override
            public PredictionResponseDTO predictWaterLevel(PredictionRequestDTO request) {
                return localArimaPredictor.predict(request);
            }

            @Override
            public PredictionResponseDTO predictPowerGeneration(PredictionRequestDTO request) {
                return localArimaPredictor.predict(request);
            }
        };
    }
}
