package com.huadianguangdong.analysis.predict;

import com.huadianguangdong.common.dto.AlertEventDTO;
import com.huadianguangdong.common.dto.PredictionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 趋势预警触发器
 * <p>
 * 检查预测结果是否触发 TREND_WARNING 规则：
 * 预测最大值超过趋势预警阈值时，生成 {@link AlertEventDTO} 推送到 {@code alert.event}。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrendWarningTrigger {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 趋势预警阈值（预测最高水位超过此值即触发预警） */
    @Value("${predict.trend.warning-threshold:13.0}")
    private double trendWarningThreshold;

    /**
     * 检查预测结果并触发 TREND_WARNING
     *
     * @param response 预测结果
     */
    public void triggerTrendWarning(PredictionResponseDTO response) {
        if (response == null || response.getForecastValues() == null || response.getForecastValues().isEmpty()) {
            return;
        }

        // 仅对水位预测触发趋势预警
        if (!"water_level".equals(response.getPredictType())) {
            return;
        }

        Double maxForecast = response.getMaxForecast();
        if (maxForecast == null) {
            return;
        }

        if (maxForecast > trendWarningThreshold) {
            log.info("[趋势预警] 预测最高水位 {} 超过阈值 {}，触发 TREND_WARNING",
                    String.format("%.2f", maxForecast), trendWarningThreshold);

            AlertEventDTO alert = AlertEventDTO.builder()
                    .ruleId(999L)
                    .ruleName("趋势预警-水位上升趋势")
                    .logicType("trend_warning")
                    .ruleType("hydro")
                    .stationId(response.getTargetId())
                    .level(maxForecast > trendWarningThreshold + 2.0 ? "high" : "medium")
                    .content(String.format("水位趋势预警：未来预测最高水位 %.2f m，超过趋势阈值 %.2f m",
                            maxForecast, trendWarningThreshold))
                    .metric("water_level")
                    .metricValue(maxForecast)
                    .threshold(trendWarningThreshold)
                    .triggerTime(LocalDateTime.now())
                    .build();

            kafkaTemplate.send("alert.event", String.valueOf(response.getTargetId()), alert);
        }
    }
}
