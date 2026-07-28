package com.huadianguangdong.collector.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.dto.WeatherDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 气象数据解析与清洗器
 * <p>
 * 职责：
 * <ol>
 *   <li>解析广东省气象局 API 返回的 JSON</li>
 *   <li>按厂区代码映射到对应电厂</li>
 *   <li>清洗异常值（温度 > 60℃ 标记为 null、湿度超 0-100 置 null 等）</li>
 * </ol>
 * <p>
 * 清洗阈值通过 {@link WeatherCleaningProperties} 注入，支持 application.yml 动态配置。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherDataParser {

    private final ObjectMapper objectMapper;
    private final WeatherCleaningProperties cleaningProps;

    /**
     * 解析广东省气象局 API 返回 JSON，生成清洗后的 DTO 列表
     * <p>
     * 预期 JSON 结构：
     * <pre>
     * {
     *   "code": 200,
     *   "data": [
     *     {
     *       "stationCode": "GD001",
     *       "city": "汕头",
     *       "temp": "28.5",
     *       "humidity": "75",
     *       "windSpeed": "5.2",
     *       "windDir": "180",
     *       "windDirection": "南风",
     *       "rain1h": "0.0",
     *       "pressure": "1013.2",
     *       "observeTime": "2026-07-27 10:00:00"
     *     }
     *   ]
     * }
     * </pre>
     *
     * @param rawJson 原始 JSON 字符串
     * @return 清洗后的气象数据列表（无法映射电厂的记录会被跳过）
     */
    public List<WeatherDataDTO> parse(String rawJson) {
        List<WeatherDataDTO> result = new ArrayList<>();
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("[气象解析] 原始 JSON 为空");
            return result;
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            int code = root.path("code").asInt(-1);
            if (code != 200) {
                log.warn("[气象解析] API 返回非成功状态 code={}", code);
                return result;
            }

            JsonNode dataArray = root.path("data");
            if (!dataArray.isArray()) {
                log.warn("[气象解析] data 字段非数组");
                return result;
            }

            for (JsonNode item : dataArray) {
                WeatherDataDTO dto = parseItem(item);
                if (dto != null) {
                    result.add(dto);
                }
            }
        } catch (Exception e) {
            log.error("[气象解析] JSON 解析失败: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 解析单条气象记录 + 厂区映射 + 异常值清洗
     */
    private WeatherDataDTO parseItem(JsonNode item) {
        String stationCode = item.path("stationCode").asText("");
        String city = item.path("city").asText("");

        // 1. 映射到电厂
        var mappingOpt = PlantCodeMapper.findByCode(stationCode)
                .or(() -> PlantCodeMapper.findByCity(city));
        if (mappingOpt.isEmpty()) {
            log.debug("[气象解析] 无法映射电厂 stationCode={} city={}", stationCode, city);
            return null;
        }
        var mapping = mappingOpt.get();

        // 2. 解析 + 清洗（阈值来自 WeatherCleaningProperties，可配置）
        Float temp = cleanFloat(item, "temp", cleaningProps.getTempMin(), cleaningProps.getTempMax());
        Float humidity = cleanFloat(item, "humidity", cleaningProps.getHumidityMin(), cleaningProps.getHumidityMax());
        Float windSpeed = cleanFloat(item, "windSpeed", cleaningProps.getWindSpeedMin(), cleaningProps.getWindSpeedMax());
        Short windDir = cleanShort(item, "windDir", cleaningProps.getWindDirMin(), cleaningProps.getWindDirMax());
        Float rain1h = cleanFloat(item, "rain1h", cleaningProps.getRainMin(), cleaningProps.getRainMax());
        Float pressure = cleanFloat(item, "pressure", cleaningProps.getPressureMin(), cleaningProps.getPressureMax());
        String windDirection = item.path("windDirection").asText(null);
        String observeTime = item.path("observeTime").asText("");

        // 3. 组装 DTO
        return WeatherDataDTO.builder()
                .plantId(mapping.plantId())
                .districtCode(mapping.districtCode())
                .temp(temp)
                .humidity(humidity)
                .windSpeed(windSpeed)
                .windDir(windDir)
                .windDirection(windDirection)
                .rain1h(rain1h)
                .rainfall(rain1h)  // 兼容旧字段
                .pressure(pressure)
                .ts(observeTime.isBlank() ? LocalDateTime.now().toString() : observeTime)
                .build();
    }

    /**
     * 清洗浮点值：超出合理范围返回 null
     * <p>
     * 当 {@code cleaningProps.enabled = false} 时，跳过清洗直接返回原始解析值。
     */
    private Float cleanFloat(JsonNode node, String field, float min, float max) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        try {
            float val = Float.parseFloat(v.asText());
            if (!cleaningProps.isEnabled()) {
                return val;
            }
            if (val < min || val > max) {
                log.warn("[气象清洗] 字段={} 值={} 超出范围[{},{}]，标记为 null", field, val, min, max);
                return null;
            }
            return val;
        } catch (NumberFormatException e) {
            log.warn("[气象清洗] 字段={} 值={} 解析失败", field, v.asText());
            return null;
        }
    }

    /**
     * 清洗短整型值
     */
    private Short cleanShort(JsonNode node, String field, short min, short max) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        try {
            short val = Short.parseShort(v.asText());
            if (!cleaningProps.isEnabled()) {
                return val;
            }
            if (val < min || val > max) {
                log.warn("[气象清洗] 字段={} 值={} 超出范围[{},{}]，标记为 null", field, val, min, max);
                return null;
            }
            return val;
        } catch (NumberFormatException e) {
            log.warn("[气象清洗] 字段={} 值={} 解析失败", field, v.asText());
            return null;
        }
    }
}
