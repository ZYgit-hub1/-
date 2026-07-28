package com.huadianguangdong.collector.weather;

import com.huadianguangdong.collector.tdengine.entity.WeatherLive;
import com.huadianguangdong.collector.tdengine.repository.TdengineRepository;
import com.huadianguangdong.common.dto.WeatherDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 气象数据 → TDengine 写入适配器
 * <p>
 * 将 WeatherDataDTO 转换为 TDengine WeatherLive 实体并批量写入。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherTdengineWriter {

    private final TdengineRepository tdengineRepository;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 批量写入气象数据到 TDengine weather_live
     *
     * @param dtoList 清洗后的气象数据列表
     * @return 实际写入条数
     */
    public int write(List<WeatherDataDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return 0;
        }

        // 按电厂 ID 分组写入（每个电厂一张子表）
        List<WeatherLive> batch = new ArrayList<>();
        Long currentPlantId = null;

        int successCount = 0;
        for (WeatherDataDTO dto : dtoList) {
            if (dto.getPlantId() == null) {
                continue;
            }

            // 电厂 ID 变化时，先刷入上一批
            if (currentPlantId != null && !currentPlantId.equals(dto.getPlantId()) && !batch.isEmpty()) {
                successCount += flushBatch(batch);
                batch.clear();
            }
            currentPlantId = dto.getPlantId();

            WeatherLive entity = convert(dto);
            batch.add(entity);
        }

        // 刷入最后一批
        if (!batch.isEmpty()) {
            successCount += flushBatch(batch);
        }

        log.info("[TDengine写入] 气象数据写入完成，成功 {} 条", successCount);
        return successCount;
    }

    /**
     * 刷入一批（同一电厂）
     */
    private int flushBatch(List<WeatherLive> batch) {
        try {
            tdengineRepository.batchInsertWeather(batch);
            return batch.size();
        } catch (Exception e) {
            log.error("[TDengine写入] 批量写入失败 count={} err={}", batch.size(), e.getMessage(), e);
            // 降级：逐条写入
            int count = 0;
            for (WeatherLive w : batch) {
                try {
                    tdengineRepository.insertWeather(w);
                    count++;
                } catch (Exception ex) {
                    log.error("[TDengine写入] 单条写入失败 plantId={} ts={}", w.getPlantId(), w.getTs(), ex);
                }
            }
            return count;
        }
    }

    /**
     * DTO → TDengine Entity 转换
     */
    private WeatherLive convert(WeatherDataDTO dto) {
        WeatherLive entity = new WeatherLive();
        entity.setPlantId(dto.getPlantId());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setTs(parseTimestamp(dto.getTs()));
        entity.setTemp(dto.getTemp());
        entity.setHumidity(dto.getHumidity());
        entity.setWindSpeed(dto.getWindSpeed());
        entity.setWindDir(dto.getWindDir());
        entity.setRain1h(dto.getRain1h());
        entity.setPressure(dto.getPressure());
        return entity;
    }

    /**
     * 解析时间戳（支持 ISO 8601 和 yyyy-MM-dd HH:mm:ss 格式）
     */
    private LocalDateTime parseTimestamp(String ts) {
        if (ts == null || ts.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            if (ts.contains("T")) {
                return LocalDateTime.parse(ts);
            }
            return LocalDateTime.parse(ts, TS_FMT);
        } catch (DateTimeParseException e) {
            log.warn("[TDengine写入] 时间戳解析失败 ts={}，使用当前时间", ts);
            return LocalDateTime.now();
        }
    }
}
