package com.huadianguangdong.analysis.config;

import com.huadianguangdong.analysis.predict.LocalArimaPredictor;
import com.huadianguangdong.analysis.predict.fallback.PythonPredictFeignFallbackFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置
 * <p>
 * 注册预测服务降级工厂 Bean。
 *
 * @author huadianguangdong
 */
@Configuration
public class FeignConfig {

    @Bean
    public PythonPredictFeignFallbackFactory pythonPredictFallbackFactory(LocalArimaPredictor localArimaPredictor) {
        return new PythonPredictFeignFallbackFactory(localArimaPredictor);
    }
}
