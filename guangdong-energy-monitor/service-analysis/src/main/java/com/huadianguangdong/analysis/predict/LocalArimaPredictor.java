package com.huadianguangdong.analysis.predict;

import com.huadianguangdong.common.dto.PredictionRequestDTO;
import com.huadianguangdong.common.dto.PredictionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地 ARIMA 轻量预测器（降级方案）
 * <p>
 * 当 Python FastAPI 服务不可用时，使用简单的线性趋势 + 季节性衰减模型作为降级预测。
 * 算法：
 * <ol>
 *   <li>从历史数据中提取线性趋势（最小二乘法）</li>
 *   <li>计算最近 N 个周期的残差标准差（波动率）</li>
 *   <li>对预测区间应用趋势外推 + 波动率衰减</li>
 * </ol>
 * <p>
 * 注意：此实现仅作为降级兜底，精度远低于 Python LSTM/XGBoost 模型。
 * 生产环境应确保 Python 服务高可用，降级仅在极端情况下生效。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class LocalArimaPredictor {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 默认预测步长（小时） */
    private static final int DEFAULT_FORECAST_HOURS = 24;

    /** ARIMA 简化：使用最近 3 个周期的加权平均作为趋势斜率 */
    private static final int TREND_WINDOW = 6;

    /** 衰减因子（越远的预测点波动越小） */
    private static final double DECAY_FACTOR = 0.95;

    /**
     * 执行降级预测
     *
     * @param request 预测请求
     * @return 预测结果（标记 fallback=true）
     */
    public PredictionResponseDTO predict(PredictionRequestDTO request) {
        List<Double> history = request.getHistoryData();
        if (history == null || history.size() < 2) {
            log.warn("[ARIMA降级] 历史数据不足（<2条），返回空预测");
            return PredictionResponseDTO.builder()
                    .predictType(request.getPredictType())
                    .targetId(request.getStationId() != null ? request.getStationId() : request.getPlantId())
                    .forecastValues(new ArrayList<>())
                    .forecastTimes(new ArrayList<>())
                    .upperBound(new ArrayList<>())
                    .lowerBound(new ArrayList<>())
                    .confidence(0.0)
                    .fallback(true)
                    .modelUsed("arima_local_insufficient")
                    .generatedAt(LocalDateTime.now().format(TS_FMT))
                    .build();
        }

        int forecastHours = request.getForecastHours() != null ? request.getForecastHours() : DEFAULT_FORECAST_HOURS;
        Long targetId = request.getStationId() != null ? request.getStationId() : request.getPlantId();

        // 1. 提取趋势
        double slope = estimateTrend(history);
        // 2. 提取波动率
        double volatility = estimateVolatility(history, slope);
        // 3. 生成预测序列
        double lastValue = history.get(history.size() - 1);
        LocalDateTime lastTime = parseLastTime(request.getHistoryTimes());

        List<String> forecastTimes = new ArrayList<>(forecastHours);
        List<Double> forecastValues = new ArrayList<>(forecastHours);
        List<Double> upperBound = new ArrayList<>(forecastHours);
        List<Double> lowerBound = new ArrayList<>(forecastHours);
        double decay = 1.0;

        for (int i = 1; i <= forecastHours; i++) {
            double predicted = lastValue + slope * i + volatility * decay * 0.5;
            // 防止负值（水位/发电量不应为负）
            predicted = Math.max(0, predicted);
            double ci = volatility * decay * 1.96; // 95% 置信区间宽度
            double ub = Math.round((predicted + ci) * 100.0) / 100.0;
            double lb = Math.round(Math.max(0, predicted - ci) * 100.0) / 100.0;

            forecastValues.add(Math.round(predicted * 100.0) / 100.0);
            upperBound.add(ub);
            lowerBound.add(lb);
            forecastTimes.add(lastTime.plusHours(i).format(TS_FMT));
            decay *= DECAY_FACTOR;
        }

        log.info("[ARIMA降级] 完成 {} 预测 targetId={} forecastHours={} slope={}",
                request.getPredictType(), targetId, forecastHours, String.format("%.4f", slope));

        return PredictionResponseDTO.builder()
                .predictType(request.getPredictType())
                .targetId(targetId)
                .forecastTimes(forecastTimes)
                .forecastValues(forecastValues)
                .upperBound(upperBound)
                .lowerBound(lowerBound)
                .confidence(0.5)  // 降级模型置信度较低
                .modelUsed("arima_local")
                .fallback(true)
                .generatedAt(LocalDateTime.now().format(TS_FMT))
                .build();
    }

    /**
     * 估算线性趋势斜率（最小二乘法）
     */
    private double estimateTrend(List<Double> data) {
        int n = Math.min(data.size(), TREND_WINDOW);
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int start = data.size() - n;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = data.get(start + i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return 0;
        }
        return (n * sumXY - sumX * sumY) / denominator;
    }

    /**
     * 估算波动率（残差标准差）
     */
    private double estimateVolatility(List<Double> data, double slope) {
        int n = Math.min(data.size(), TREND_WINDOW);
        int start = data.size() - n;
        double sumSqErr = 0;
        for (int i = 0; i < n; i++) {
            double expected = data.get(start) + slope * i;
            double err = data.get(start + i) - expected;
            sumSqErr += err * err;
        }
        return Math.sqrt(sumSqErr / n);
    }

    /**
     * 解析最后一个时间点
     */
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
