package com.huadianguangdong.analysis.predict;

import com.huadianguangdong.common.dto.PredictionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 预测结果 TDengine 写入器
 * <p>
 * 将预测结果写入 TDengine {@code prediction_result} 超级表。
 * 每条预测时间点一行，tag 为 target_id + predict_type。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
public class PredictionResultWriter {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource tdengineDataSource;

    @Value("${predict.tdengine.batch-size:50}")
    private int batchSize;

    public PredictionResultWriter(@Qualifier("tdengineDataSource") DataSource tdengineDataSource) {
        this.tdengineDataSource = tdengineDataSource;
    }

    /**
     * 写入水位预测结果到 TDengine
     *
     * @param response 预测结果
     */
    public void writeWaterLevelResult(PredictionResponseDTO response) {
        if (response == null || response.getForecastTimes() == null || response.getForecastTimes().isEmpty()) {
            log.warn("[预测写入] 水位预测结果为空，跳过写入");
            return;
        }
        writeBatch(response, "water_level", response.getTargetId());
    }

    /**
     * 写入发电预测结果到 TDengine
     *
     * @param response 预测结果
     */
    public void writePowerResult(PredictionResponseDTO response) {
        if (response == null || response.getForecastTimes() == null || response.getForecastTimes().isEmpty()) {
            log.warn("[预测写入] 发电预测结果为空，跳过写入");
            return;
        }
        writeBatch(response, "power_generation", response.getTargetId());
    }

    /**
     * 批量写入预测结果
     */
    private void writeBatch(PredictionResponseDTO response, String predictType, Long targetId) {
        List<String> times = response.getForecastTimes();
        List<Double> values = response.getForecastValues();
        List<Double> upperBound = response.getUpperBound();
        List<Double> lowerBound = response.getLowerBound();

        String tableName = "prediction_result_" + targetId;
        String generatedAt = response.getGeneratedAt() != null ? response.getGeneratedAt() :
                LocalDateTime.now().format(TS_FMT);

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" USING prediction_result TAGS (").append(targetId)
                .append(", '").append(predictType).append("') VALUES ");

        for (int i = 0; i < times.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            double ub = (upperBound != null && i < upperBound.size()) ? upperBound.get(i) : values.get(i);
            double lb = (lowerBound != null && i < lowerBound.size()) ? lowerBound.get(i) : values.get(i);

            sql.append("('").append(times.get(i)).append("', ")
                    .append(values.get(i)).append(", ")
                    .append(ub).append(", ")
                    .append(lb).append(", ")
                    .append(response.getConfidence() != null ? response.getConfidence() : 0.5).append(", ")
                    .append(response.isFallback() ? "true" : "false").append(", ")
                    .append("'").append(response.getModelUsed() != null ? response.getModelUsed() : "unknown").append("', ")
                    .append("'").append(generatedAt).append("')");
        }

        try (Connection conn = tdengineDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int rows = ps.executeUpdate();
            log.info("[预测写入] 写入 prediction_result 成功 tableName={} rows={} fallback={}",
                    tableName, rows, response.isFallback());
        } catch (SQLException e) {
            log.error("[预测写入] 写入 TDengine 失败 tableName={} err={}", tableName, e.getMessage(), e);
        }
    }
}
