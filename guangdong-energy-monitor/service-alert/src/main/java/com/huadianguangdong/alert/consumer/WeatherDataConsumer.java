package com.huadianguangdong.alert.consumer;

import com.huadianguangdong.alert.service.AlertEventRouter;
import com.huadianguangdong.alert.service.RuleEngineService;
import com.huadianguangdong.common.constant.KafkaTopics;
import com.huadianguangdong.common.dto.AlertEventDTO;
import com.huadianguangdong.common.dto.WeatherRawMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 气象数据 Kafka 消费者
 * <p>
 * 消费 {@link KafkaTopics#WEATHER_RAW} 主题的气象原始消息（含 source/ts/raw/cleaned），
 * 注入 Drools WorkingMemory 匹配规则，通过抑制后路由到推送服务。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherDataConsumer {

    private final RuleEngineService ruleEngineService;
    private final AlertEventRouter alertEventRouter;

    /**
     * 消费气象原始数据
     *
     * @param record Kafka 消息记录
     * @param ack    手动提交 offset
     */
    @KafkaListener(
            topics = KafkaTopics.WEATHER_RAW,
            groupId = "${spring.kafka.consumer.group-id:alert-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, WeatherRawMessage> record, Acknowledgment ack) {
        WeatherRawMessage rawMessage = record.value();
        if (rawMessage == null || rawMessage.getCleaned() == null) {
            log.warn("[气象消费] 消息为空或 cleaned 字段缺失, offset={}", record.offset());
            ack.acknowledge();
            return;
        }

        log.info("[气象消费] 收到消息 plantId={} source={} temp={} windSpeed={} rainfall={}",
                rawMessage.getPlantId(), rawMessage.getSource(),
                rawMessage.getCleaned().getTemp(), rawMessage.getCleaned().getWindSpeed(),
                rawMessage.getCleaned().getRainfall());

        try {
            // 1. 注入 Drools WorkingMemory 匹配规则
            List<AlertEventDTO> events = ruleEngineService.executeWeatherRules(rawMessage);

            if (events.isEmpty()) {
                log.debug("[气象消费] 无规则匹配 plantId={}", rawMessage.getPlantId());
            } else {
                log.info("[气象消费] 规则匹配 {} 条事件 plantId={}", events.size(), rawMessage.getPlantId());

                // 2. 路由到持久化 + Kafka + 推送
                alertEventRouter.route(events);
            }

        } catch (Exception e) {
            log.error("[气象消费] 处理异常 plantId={}", rawMessage.getPlantId(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
