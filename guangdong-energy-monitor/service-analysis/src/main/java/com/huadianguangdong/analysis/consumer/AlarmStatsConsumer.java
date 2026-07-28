package com.huadianguangdong.analysis.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.constant.CommonConstants;
import com.huadianguangdong.common.dto.AlarmDTO;
import com.huadianguangdong.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 报警统计 Kafka 消费者
 *
 * <p>消费报警 Topic，实时更新 Redis 中的报警计数器，供驾驶舱即时展示。
 * <ul>
 *     <li>{@code analysis:alarm:total} —— 报警总数</li>
 *     <li>{@code analysis:alarm:level:{level}} —— 按级别计数</li>
 *     <li>{@code analysis:alarm:active} —— 活跃（未处理）预警数</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmStatsConsumer {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    /** 报警总数计数器 key */
    private static final String KEY_ALARM_TOTAL = "analysis:alarm:total";
    /** 活跃预警计数器 key */
    private static final String KEY_ALARM_ACTIVE = "analysis:alarm:active";
    /** 按级别计数器 key 前缀 */
    private static final String KEY_ALARM_LEVEL_PREFIX = "analysis:alarm:level:";

    @KafkaListener(topics = CommonConstants.TOPIC_ALARM, groupId = "${spring.kafka.consumer.group-id:analysis-service-group}")
    public void onAlarm(String message) {
        try {
            AlarmDTO alarm = objectMapper.readValue(message, AlarmDTO.class);
            log.debug("收到报警消息: plantId={}, level={}, status={}", alarm.getPlantId(), alarm.getLevel(), alarm.getStatus());

            // 报警总数 +1
            redisUtil.incr(KEY_ALARM_TOTAL);

            // 按级别 +1
            if (alarm.getLevel() != null) {
                redisUtil.incr(KEY_ALARM_LEVEL_PREFIX + alarm.getLevel());
            }

            // 活跃预警：未处理状态 +1
            if (isUnprocessed(alarm.getStatus())) {
                redisUtil.incr(KEY_ALARM_ACTIVE);
            }
        } catch (Exception e) {
            log.error("处理报警消息失败: {}", message, e);
        }
    }

    /** 判断是否为未处理状态 */
    private boolean isUnprocessed(String status) {
        return status != null && (status.equals("PENDING") || status.equals("PROCESSING") || status.equals("WARNING"));
    }
}
