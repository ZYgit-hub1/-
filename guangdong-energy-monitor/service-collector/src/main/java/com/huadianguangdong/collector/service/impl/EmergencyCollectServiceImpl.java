package com.huadianguangdong.collector.service.impl;

import com.huadianguangdong.collector.entity.EmergencyEvent;
import com.huadianguangdong.collector.mapper.EmergencyEventMapper;
import com.huadianguangdong.collector.service.EmergencyCollectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 应急事件采集服务实现
 * <p>
 * 定时拉取应急事件并入库（当前为占位实现，实际应对接应急指挥平台 API）。
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
public class EmergencyCollectServiceImpl implements EmergencyCollectService {

    @Autowired
    private EmergencyEventMapper emergencyEventMapper;

    /**
     * 定时采集应急事件，cron 表达式可配置
     */
    @Override
    @Scheduled(cron = "${collector.emergency.cron:0 0/30 * * * ?}")
    public List<EmergencyEvent> collectAndPush() {
        log.info("[应急采集] 开始采集应急事件");
        try {
            // TODO: 对接应急指挥平台 API，拉取最新事件
            // 此处为占位实现，保证采集链路可运行
            EmergencyEvent event = buildPlaceholderEvent();
            emergencyEventMapper.insert(event);
            log.info("[应急采集] 入库成功，id={}", event.getId());
            return Collections.singletonList(event);
        } catch (Exception e) {
            log.error("[应急采集] 采集失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 构造占位应急事件（接入真实 API 后替换）
     */
    private EmergencyEvent buildPlaceholderEvent() {
        EmergencyEvent event = new EmergencyEvent();
        event.setType("weather");
        event.setLevel("watch");
        event.setTitle("台风预警");
        event.setContent("预计未来 24 小时内广东省沿海地区将有强降雨，请注意防范。");
        event.setLocation("广东省沿海地区");
        event.setLng(113.2644);
        event.setLat(23.1291);
        event.setOccurTime(LocalDateTime.now());
        event.setStatus("active");
        return event;
    }
}
