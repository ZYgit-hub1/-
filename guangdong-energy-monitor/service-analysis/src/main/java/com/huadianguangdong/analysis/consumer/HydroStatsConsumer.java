package com.huadianguangdong.analysis.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.constant.CommonConstants;
import com.huadianguangdong.common.dto.HydroDataDTO;
import com.huadianguangdong.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 水文数据统计 Kafka 消费者
 *
 * <p>消费水文数据 Topic，实时更新 Redis 中的水文聚合数据，供驾驶舱即时展示。
 * <ul>
 *     <li>{@code analysis:hydro:latest:{stationId}} —— 水文站最新水位（字符串）</li>
 *     <li>{@code analysis:hydro:alert} —— 当前告警水文站数（增量维护，按 stationId 去重见 TODO）</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HydroStatsConsumer {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    /** 水文站最新水位 key 前缀 */
    private static final String KEY_HYDRO_LATEST_PREFIX = "analysis:hydro:latest:";
    /** 水文告警计数器 key */
    private static final String KEY_HYDRO_ALERT = "analysis:hydro:alert";
    /** 最新水位缓存 TTL（分钟） */
    private static final long LATEST_TTL_MINUTES = 30;

    @KafkaListener(topics = CommonConstants.TOPIC_HYDRO_DATA, groupId = "${spring.kafka.consumer.group-id:analysis-service-group}")
    public void onHydroData(String message) {
        try {
            HydroDataDTO hydro = objectMapper.readValue(message, HydroDataDTO.class);
            log.debug("收到水文数据: stationId={}, waterLevel={}, alertLevel={}",
                    hydro.getStationId(), hydro.getWaterLevel(), hydro.getAlertLevel());

            // 更新水文站最新水位（覆盖写入，带 TTL）
            if (hydro.getStationId() != null) {
                redisUtil.setEx(KEY_HYDRO_LATEST_PREFIX + hydro.getStationId(),
                        String.valueOf(hydro.getWaterLevel()), LATEST_TTL_MINUTES, TimeUnit.MINUTES);
            }

            // 告警级别计数：存在告警级别则 +1
            // TODO: 此处为简化实现，按消息增量计数；准确去重计数应由 Flink 窗口聚合或定时任务修正。
            if (hydro.getAlertLevel() != null && !hydro.getAlertLevel().isEmpty()
                    && !hydro.getAlertLevel().equalsIgnoreCase("NORMAL")) {
                redisUtil.incr(KEY_HYDRO_ALERT);
            }
        } catch (Exception e) {
            log.error("处理水文数据失败: {}", message, e);
        }
    }
}
