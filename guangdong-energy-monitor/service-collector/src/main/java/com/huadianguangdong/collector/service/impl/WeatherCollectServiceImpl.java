package com.huadianguangdong.collector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huadianguangdong.collector.entity.WeatherData;
import com.huadianguangdong.collector.feign.WeatherApiFeignClient;
import com.huadianguangdong.collector.mapper.WeatherDataMapper;
import com.huadianguangdong.collector.service.WeatherCollectService;
import com.huadianguangdong.common.constant.CommonConstants;
import com.huadianguangdong.common.dto.WeatherDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 气象数据采集服务实现
 * <p>
 * 调用外部气象 API 获取原始 JSON，解析为 WeatherDataDTO，
 * 通过 Kafka 推送到 {@link CommonConstants#TOPIC_WEATHER_DATA}，并入库。
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
public class WeatherCollectServiceImpl implements WeatherCollectService {

    @Autowired
    private WeatherApiFeignClient weatherApiFeignClient;

    @Autowired
    private WeatherDataMapper weatherDataMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /** 默认采集城市（可通过配置覆盖） */
    @Value("${collector.weather.city:广州}")
    private String defaultCity;

    /** 默认关联电厂 ID（可通过配置覆盖） */
    @Value("${collector.weather.plant-id:1}")
    private Long defaultPlantId;

    /**
     * 定时采集气象数据，cron 表达式可配置
     */
    @Override
    @Scheduled(cron = "${collector.weather.cron:0 0/10 * * * ?}")
    public List<WeatherData> collectAndPush() {
        log.info("[气象采集] 开始采集，city={}, plantId={}", defaultCity, defaultPlantId);
        try {
            // 1. 调用外部气象 API 获取原始 JSON
            String json = weatherApiFeignClient.getWeather(defaultCity);
            log.debug("[气象采集] 外部返回原始 JSON：{}", json);

            // 2. 解析为 DTO（这里简化为构造示例数据，实际应使用 JSON 工具解析 json）
            WeatherDataDTO dto = parseWeatherJson(json, defaultPlantId);

            // 3. 通过 Kafka 推送
            kafkaTemplate.send(CommonConstants.TOPIC_WEATHER_DATA, dto);
            log.info("[气象采集] 已发送到 Kafka topic={}", CommonConstants.TOPIC_WEATHER_DATA);

            // 4. 入库
            WeatherData entity = convertToEntity(dto);
            weatherDataMapper.insert(entity);
            log.info("[气象采集] 入库成功，id={}", entity.getId());

            return Collections.singletonList(entity);
        } catch (Exception e) {
            log.error("[气象采集] 采集失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public WeatherData getLatestByPlantId(Long plantId) {
        LambdaQueryWrapper<WeatherData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeatherData::getPlantId, plantId)
                .orderByDesc(WeatherData::getRecordTime)
                .last("LIMIT 1");
        return weatherDataMapper.selectOne(wrapper);
    }

    /**
     * 解析外部气象 JSON 为 DTO
     * <p>
     * TODO：接入真实 API 后，使用 Hutool/Jackson 按 API 字段映射解析。
     * 此处给出占位实现，保证采集链路可运行。
     */
    private WeatherDataDTO parseWeatherJson(String json, Long plantId) {
        WeatherDataDTO dto = new WeatherDataDTO();
        dto.setPlantId(plantId);
        dto.setTemp(26.5);
        dto.setHumidity(72.0);
        dto.setWindSpeed(3.2);
        dto.setWindDirection("东南风");
        dto.setRainfall(0.0);
        return dto;
    }

    /**
     * DTO 转实体
     */
    private WeatherData convertToEntity(WeatherDataDTO dto) {
        WeatherData entity = new WeatherData();
        entity.setPlantId(dto.getPlantId());
        entity.setTemp(dto.getTemp());
        entity.setHumidity(dto.getHumidity());
        entity.setWindSpeed(dto.getWindSpeed());
        entity.setWindDirection(dto.getWindDirection());
        entity.setRainfall(dto.getRainfall());
        entity.setRecordTime(LocalDateTime.now());
        return entity;
    }
}
