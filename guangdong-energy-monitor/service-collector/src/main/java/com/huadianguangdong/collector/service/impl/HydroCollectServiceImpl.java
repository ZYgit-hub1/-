package com.huadianguangdong.collector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huadianguangdong.collector.entity.HydroData;
import com.huadianguangdong.collector.feign.HydroApiFeignClient;
import com.huadianguangdong.collector.mapper.HydroDataMapper;
import com.huadianguangdong.collector.service.HydroCollectService;
import com.huadianguangdong.common.constant.CommonConstants;
import com.huadianguangdong.common.dto.HydroDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 水文数据采集服务实现
 * <p>
 * 调用外部水文 API 获取原始 JSON，解析为 HydroDataDTO，
 * 通过 Kafka 推送到 {@link CommonConstants#TOPIC_HYDRO_DATA}，并入库。
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
public class HydroCollectServiceImpl implements HydroCollectService {

    private static final DateTimeFormatter READING_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private HydroApiFeignClient hydroApiFeignClient;

    @Autowired
    private HydroDataMapper hydroDataMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /** 默认水文站编码（可通过配置覆盖） */
    @Value("${collector.hydro.station-code:GD001}")
    private String defaultStationCode;

    /** 默认关联水文站 ID（可通过配置覆盖） */
    @Value("${collector.hydro.station-id:1}")
    private Long defaultStationId;

    /**
     * 定时采集水文数据，cron 表达式可配置
     */
    @Override
    @Scheduled(cron = "${collector.hydro.cron:0 0/5 * * * ?}")
    public List<HydroData> collectAndPush() {
        log.info("[水文采集] 开始采集，stationCode={}, stationId={}", defaultStationCode, defaultStationId);
        try {
            // 1. 调用外部水文 API 获取原始 JSON
            String json = hydroApiFeignClient.getWaterLevel(defaultStationCode);
            log.debug("[水文采集] 外部返回原始 JSON：{}", json);

            // 2. 解析为 DTO（占位实现，实际应使用 JSON 工具解析 json）
            HydroDataDTO dto = parseHydroJson(json, defaultStationId);

            // 3. 通过 Kafka 推送
            kafkaTemplate.send(CommonConstants.TOPIC_HYDRO_DATA, dto);
            log.info("[水文采集] 已发送到 Kafka topic={}", CommonConstants.TOPIC_HYDRO_DATA);

            // 4. 入库
            HydroData entity = convertToEntity(dto);
            hydroDataMapper.insert(entity);
            log.info("[水文采集] 入库成功，id={}", entity.getId());

            return Collections.singletonList(entity);
        } catch (Exception e) {
            log.error("[水文采集] 采集失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<HydroData> listReadings(Long stationId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<HydroData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HydroData::getStationId, stationId)
                .ge(startTime != null, HydroData::getReadingTime, startTime)
                .le(endTime != null, HydroData::getReadingTime, endTime)
                .orderByDesc(HydroData::getReadingTime);
        return hydroDataMapper.selectList(wrapper);
    }

    /**
     * 解析外部水文 JSON 为 DTO
     * <p>
     * TODO：接入真实 API 后，使用 Hutool/Jackson 按 API 字段映射解析。
     * 此处给出占位实现，保证采集链路可运行。
     */
    private HydroDataDTO parseHydroJson(String json, Long stationId) {
        LocalDateTime now = LocalDateTime.now();
        HydroDataDTO dto = new HydroDataDTO();
        dto.setStationId(stationId);
        dto.setWaterLevel(12.8);
        dto.setFlowRate(350.0);
        dto.setTrend("steady");
        dto.setAlertLevel("normal");
        dto.setReadingTime(now.format(READING_TIME_FMT));
        return dto;
    }

    /**
     * DTO 转实体
     */
    private HydroData convertToEntity(HydroDataDTO dto) {
        HydroData entity = new HydroData();
        entity.setStationId(dto.getStationId());
        entity.setWaterLevel(dto.getWaterLevel());
        entity.setFlowRate(dto.getFlowRate());
        entity.setTrend(dto.getTrend());
        entity.setAlertLevel(dto.getAlertLevel());
        entity.setReadingTime(LocalDateTime.now());
        return entity;
    }
}
