package com.huadianguangdong.analysis.predict.impl;

import com.huadianguangdong.analysis.mapper.PredictMapper;
import com.huadianguangdong.analysis.predict.LocalArimaPredictor;
import com.huadianguangdong.analysis.predict.PredictionResultWriter;
import com.huadianguangdong.analysis.predict.PredictService;
import com.huadianguangdong.analysis.predict.PythonPredictFeignClient;
import com.huadianguangdong.analysis.predict.TrendWarningTrigger;
import com.huadianguangdong.common.constant.KafkaTopics;
import com.huadianguangdong.common.dto.PredictionRequestDTO;
import com.huadianguangdong.common.dto.PredictionResponseDTO;
import com.huadianguangdong.common.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 预测服务实现（集成降级 + TDengine 写入 + TREND_WARNING 触发 + 监控）
 * <p>
 * 流程：
 * <ol>
 *   <li>从本地 DB 查询历史数据（水位 / 气象）</li>
 *   <li>组装 {@link PredictionRequestDTO}</li>
 *   <li>调用 Python FastAPI 服务（Feign），失败时自动降级至 {@link LocalArimaPredictor}</li>
 *   <li>记录 Micrometer 指标（调用耗时 / 降级次数 / 预测值范围）</li>
 *   <li>写入 TDengine {@code prediction_result} 表</li>
 *   <li>通过 Kafka 推送至 {@link KafkaTopics#TREND_WARNING}，触发 Drools TREND_WARNING 规则</li>
 * </ol>
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictServiceImpl implements PredictService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 冷启动最小数据量：历史数据少于此值时使用均值外推（不抛异常） */
    private static final int COLD_START_MIN_POINTS = 6;

    private final PythonPredictFeignClient pythonPredictFeignClient;
    private final PredictionResultWriter predictionResultWriter;
    private final TrendWarningTrigger trendWarningTrigger;
    private final PredictMapper predictMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${predict.water-level.history-hours:24}")
    private int waterLevelHistoryHours;

    @Value("${predict.power.history-hours:24}")
    private int powerHistoryHours;

    @Value("${predict.forecast-hours:24}")
    private int defaultForecastHours;

    @Value("${predict.model-type:lstm}")
    private String defaultModelType;

    @Override
    public PredictionResponseDTO predictWaterLevel(Long stationId) {
        return predictWaterLevel(stationId, defaultForecastHours, defaultModelType);
    }

    /**
     * 水位预测（支持自定义 forecastHours / modelType）
     */
    public PredictionResponseDTO predictWaterLevel(Long stationId, Integer forecastHours, String modelType) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("[水位预测] 开始 stationId={} forecastHours={} modelType={}", stationId, forecastHours, modelType);

        try {
            List<Map<String, Object>> rows = predictMapper.selectWaterLevelHistory(stationId, waterLevelHistoryHours);
            if (rows == null || rows.isEmpty()) {
                log.warn("[水位预测] 冷启动：水文站[{}]无历史数据，返回空预测", stationId);
                recordMetrics(sample, buildColdStartResponse("water_level", stationId), "water_level", true);
                return buildColdStartResponse("water_level", stationId);
            }

            List<Double> historyData = new ArrayList<>(rows.size());
            List<String> historyTimes = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                historyTimes.add(String.valueOf(row.get("reading_time")));
                Object v = row.get("water_level");
                historyData.add(v == null ? 0.0 : ((Number) v).doubleValue());
            }

            // 冷启动检测：数据量不足时使用均值外推（不走 Python 服务，避免低质量模型污染）
            if (historyData.size() < COLD_START_MIN_POINTS) {
                log.warn("[水位预测] 冷启动：水文站[{}]仅有{}条历史数据（<{}），使用均值外推",
                        stationId, historyData.size(), COLD_START_MIN_POINTS);
                PredictionResponseDTO response = buildColdStartMeanForecast(
                        "water_level", stationId, historyData, historyTimes, forecastHours);
                recordMetrics(sample, response, "water_level", true);
                return response;
            }

            PredictionRequestDTO request = PredictionRequestDTO.forWaterLevel(
                    stationId, historyData, historyTimes, forecastHours, modelType);

            // 调用预测服务（Feign）+ 自动降级
            PredictionResponseDTO response = pythonPredictFeignClient.predictWaterLevel(request);

            // 写入 TDengine
            predictionResultWriter.writeWaterLevelResult(response);

            // 触发 TREND_WARNING 检查
            trendWarningTrigger.triggerTrendWarning(response);

            // 推送到 Kafka
            kafkaTemplate.send(KafkaTopics.TREND_WARNING, String.valueOf(stationId), response);

            // 记录指标
            recordMetrics(sample, response, "water_level", true);

            log.info("[水位预测] 完成 stationId={} fallback={} modelUsed={}",
                    stationId, response.isFallback(), response.getModelUsed());
            return response;

        } catch (Exception e) {
            recordMetrics(sample, null, "water_level", false);
            log.error("[水位预测] 失败 stationId={}", stationId, e);
            throw new BusinessException("水位预测失败: " + e.getMessage());
        }
    }

    @Override
    public PredictionResponseDTO predictPower(Long plantId) {
        return predictPower(plantId, defaultForecastHours, defaultModelType);
    }

    /**
     * 发电预测（支持自定义 forecastHours / modelType）
     */
    public PredictionResponseDTO predictPower(Long plantId, Integer forecastHours, String modelType) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("[发电预测] 开始 plantId={} forecastHours={} modelType={}", plantId, forecastHours, modelType);

        try {
            List<Map<String, Object>> rows = predictMapper.selectWeatherHistory(plantId, powerHistoryHours);
            if (rows == null || rows.isEmpty()) {
                log.warn("[发电预测] 冷启动：电厂[{}]无气象数据，返回空预测", plantId);
                recordMetrics(sample, buildColdStartResponse("power_generation", plantId), "power_generation", true);
                return buildColdStartResponse("power_generation", plantId);
            }

            List<Double> historyData = new ArrayList<>(rows.size());
            List<String> historyTimes = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                historyTimes.add(String.valueOf(row.get("time")));
                Object temp = row.get("temp");
                historyData.add(temp == null ? 0.0 : ((Number) temp).doubleValue());
            }

            // 冷启动检测
            if (historyData.size() < COLD_START_MIN_POINTS) {
                log.warn("[发电预测] 冷启动：电厂[{}]仅有{}条历史数据（<{}），使用均值外推",
                        plantId, historyData.size(), COLD_START_MIN_POINTS);
                PredictionResponseDTO response = buildColdStartMeanForecast(
                        "power_generation", plantId, historyData, historyTimes, forecastHours);
                recordMetrics(sample, response, "power_generation", true);
                return response;
            }

            PredictionRequestDTO request = PredictionRequestDTO.forPower(
                    plantId, historyData, historyTimes, forecastHours, modelType);

            PredictionResponseDTO response = pythonPredictFeignClient.predictPowerGeneration(request);

            predictionResultWriter.writePowerResult(response);

            kafkaTemplate.send(KafkaTopics.TREND_WARNING, String.valueOf(plantId), response);

            recordMetrics(sample, response, "power_generation", true);

            log.info("[发电预测] 完成 plantId={} fallback={} modelUsed={}",
                    plantId, response.isFallback(), response.getModelUsed());
            return response;

        } catch (Exception e) {
            recordMetrics(sample, null, "power_generation", false);
            log.error("[发电预测] 失败 plantId={}", plantId, e);
            throw new BusinessException("发电预测失败: " + e.getMessage());
        }
    }

    private void recordMetrics(Timer.Sample sample, PredictionResponseDTO response, String type, boolean success) {
        if (response != null) {
            sample.stop(Timer.builder("predict.duration")
                    .tag("type", type)
                    .tag("success", String.valueOf(success))
                    .tag("fallback", String.valueOf(response.isFallback()))
                    .register(meterRegistry));

            meterRegistry.counter("predict.calls", "type", type, "success", String.valueOf(success),
                    "fallback", String.valueOf(response.isFallback())).increment();

            if (response.getForecastValues() != null && !response.getForecastValues().isEmpty()) {
                Double maxVal = response.getMaxForecast();
                Double minVal = response.getMinForecast();
                if (maxVal != null) {
                    meterRegistry.gauge("predict.forecast.max", maxVal);
                }
                if (minVal != null) {
                    meterRegistry.gauge("predict.forecast.min", minVal);
                }
            }
        } else {
            sample.stop(Timer.builder("predict.duration")
                    .tag("type", type)
                    .tag("success", "false")
                    .register(meterRegistry));
            meterRegistry.counter("predict.calls", "type", type, "success", "false").increment();
        }
    }

    // ==================== 冷启动期处理 ====================

    /**
     * 冷启动零数据响应：完全无历史数据时返回空预测（标记 cold_start）
     */
    private PredictionResponseDTO buildColdStartResponse(String predictType, Long targetId) {
        return PredictionResponseDTO.builder()
                .predictType(predictType)
                .targetId(targetId)
                .forecastValues(new ArrayList<>())
                .forecastTimes(new ArrayList<>())
                .upperBound(new ArrayList<>())
                .lowerBound(new ArrayList<>())
                .confidence(0.0)
                .modelUsed("cold_start")
                .fallback(true)
                .generatedAt(LocalDateTime.now().format(TS_FMT))
                .build();
    }

    /**
     * 冷启动均值外推：数据量不足（{@code < COLD_START_MIN_POINTS}）时，
     * 使用历史均值 + 简单波动率作为预测，置信度极低。
     * <p>
     * 返回包含 upperBound / lowerBound 的完整三组数组，前端可直接渲染置信带。
     */
    private PredictionResponseDTO buildColdStartMeanForecast(String predictType, Long targetId,
                                                              List<Double> historyData, List<String> historyTimes,
                                                              Integer forecastHours) {
        int hours = forecastHours != null ? forecastHours : defaultForecastHours;
        double mean = historyData.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double std = Math.sqrt(historyData.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .average().orElse(0.0));

        LocalDateTime lastTime = parseLastTime(historyTimes);

        List<String> forecastTimes = new ArrayList<>(hours);
        List<Double> forecastValues = new ArrayList<>(hours);
        List<Double> upperBound = new ArrayList<>(hours);
        List<Double> lowerBound = new ArrayList<>(hours);

        // 冷启动外推：预测值 = 均值（无趋势信息），置信区间 = 均值 ± 2σ（随时间线性展宽）
        for (int i = 1; i <= hours; i++) {
            double expandFactor = 1.0 + 0.05 * i; // 置信区间逐小时展宽 5%
            double ub = Math.round((mean + 2 * std * expandFactor) * 100.0) / 100.0;
            double lb = Math.round(Math.max(0, mean - 2 * std * expandFactor) * 100.0) / 100.0;
            double val = Math.round(mean * 100.0) / 100.0;

            forecastTimes.add(lastTime.plusHours(i).format(TS_FMT));
            forecastValues.add(val);
            upperBound.add(ub);
            lowerBound.add(lb);
        }

        return PredictionResponseDTO.builder()
                .predictType(predictType)
                .targetId(targetId)
                .forecastTimes(forecastTimes)
                .forecastValues(forecastValues)
                .upperBound(upperBound)
                .lowerBound(lowerBound)
                .confidence(0.2) // 冷启动极低置信度
                .modelUsed("cold_start_mean")
                .fallback(true)
                .generatedAt(LocalDateTime.now().format(TS_FMT))
                .build();
    }

    private LocalDateTime parseLastTime(List<String> times) {
        if (times == null || times.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(times.get(times.size() - 1), TS_FMT);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
