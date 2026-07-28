package com.huadianguangdong.alert.consumer;

import com.huadianguangdong.alert.fact.HydroFact;
import com.huadianguangdong.alert.service.AlertEventRouter;
import com.huadianguangdong.alert.service.RuleEngineService;
import com.huadianguangdong.common.constant.KafkaTopics;
import com.huadianguangdong.common.dto.AlertEventDTO;
import com.huadianguangdong.common.dto.HydroDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 水文数据 Kafka 消费者
 * <p>
 * 消费 {@link KafkaTopics#HYDRO_LEVEL} 主题的水文实时数据，
 * 注入 Drools WorkingMemory 匹配规则，通过抑制后路由到推送服务。
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HydroDataConsumer {

    /** 默认警戒水位（兜底，实际应从水文站配置查询） */
    private static final double DEFAULT_WARNING_LEVEL = 10.0;

    /** 默认保证水位（兜底） */
    private static final double DEFAULT_GUARANTEE_LEVEL = 15.0;

    private final RuleEngineService ruleEngineService;
    private final AlertEventRouter alertEventRouter;

    /**
     * 消费水文数据
     *
     * @param record Kafka 消息记录
     * @param ack    手动提交 offset
     */
    @KafkaListener(
            topics = KafkaTopics.HYDRO_LEVEL,
            groupId = "${spring.kafka.consumer.group-id:alert-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, HydroDataDTO> record, Acknowledgment ack) {
        HydroDataDTO hydroData = record.value();
        if (hydroData == null) {
            log.warn("[水文消费] 消息为空, offset={}", record.offset());
            ack.acknowledge();
            return;
        }

        log.info("[水文消费] 收到消息 stationId={} waterLevel={} alertLevel={}",
                hydroData.getStationId(), hydroData.getWaterLevel(), hydroData.getAlertLevel());

        try {
            // TODO 通过 Feign 调用 service-plant 查询水文站阈值（warningLevel / guaranteeLevel）
            double warningLevel = DEFAULT_WARNING_LEVEL;
            double guaranteeLevel = DEFAULT_GUARANTEE_LEVEL;

            // 1. 构造 Fact 注入 Drools WorkingMemory
            HydroFact fact = new HydroFact(hydroData, warningLevel, guaranteeLevel);
            List<AlertEventDTO> events = ruleEngineService.executeHydroRulesWithResult(fact);

            if (events.isEmpty()) {
                log.debug("[水文消费] 无规则匹配 stationId={}", hydroData.getStationId());
            } else {
                log.info("[水文消费] 规则匹配 {} 条事件 stationId={}", events.size(), hydroData.getStationId());

                // 2. 路由到持久化 + Kafka + 推送
                alertEventRouter.route(events);
            }

        } catch (Exception e) {
            log.error("[水文消费] 处理异常 stationId={}", hydroData.getStationId(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
