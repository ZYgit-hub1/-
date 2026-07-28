package com.huadianguangdong.alert.service.impl;

import com.huadianguangdong.alert.entity.AlertRecord;
import com.huadianguangdong.alert.mapper.AlertRecordMapper;
import com.huadianguangdong.alert.notifier.Notifier;
import com.huadianguangdong.alert.service.AlertEventRouter;
import com.huadianguangdong.common.constant.KafkaTopics;
import com.huadianguangdong.common.dto.AlertEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报警事件路由服务实现
 * <p>
 * 持久化 → Kafka 推送 → 多通道通知
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEventRouterImpl implements AlertEventRouter {

    private final AlertRecordMapper alertRecordMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired(required = false)
    private List<Notifier> notifiers;

    @Override
    public void route(List<AlertEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (AlertEventDTO event : events) {
            route(event);
        }
    }

    @Override
    public void route(AlertEventDTO event) {
        if (event == null) {
            return;
        }

        // 1. 持久化到 t_alert_record
        AlertRecord record = convertToRecord(event);
        try {
            alertRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("[路由] t_alert_record 写入失败 ruleId={} plantId={}", event.getRuleId(), event.getPlantId(), e);
        }

        // 2. 推送到 Kafka alert.event 主题（供推送服务 / 分析服务消费）
        try {
            String key = event.getPlantId() != null ? String.valueOf(event.getPlantId()) : event.getRuleId().toString();
            kafkaTemplate.send(KafkaTopics.ALERT_EVENT, key, event);
        } catch (Exception e) {
            log.error("[路由] Kafka 推送失败 ruleId={} plantId={}", event.getRuleId(), event.getPlantId(), e);
        }

        // 3. 未被抑制的事件立即触发多通道推送
        if (!event.isSuppressed()) {
            pushNotification(event, record);
        } else {
            log.info("[路由] 事件被抑制，跳过推送 ruleId={} plantId={} aggId={}",
                    event.getRuleId(), event.getPlantId(), event.getAggregationId());
        }
    }

    /**
     * 多通道推送
     */
    private void pushNotification(AlertEventDTO event, AlertRecord record) {
        if (notifiers == null || notifiers.isEmpty()) {
            return;
        }

        // 构造 Alarm 供 Notifier 使用（兼容现有 Notifier 接口）
        com.huadianguangdong.alert.entity.Alarm alarm = new com.huadianguangdong.alert.entity.Alarm();
        alarm.setId(record.getId());
        alarm.setLevel(event.getLevel());
        alarm.setStatus("unconfirmed");
        alarm.setContent(event.getContent());
        alarm.setPlantId(event.getPlantId());
        alarm.setTriggerTime(event.getTriggerTime());

        for (Notifier notifier : notifiers) {
            try {
                notifier.notify(alarm);
            } catch (Exception e) {
                log.error("[路由] 通知通道推送失败 channel={} alarmId={}", notifier.channel(), record.getId(), e);
            }
        }
    }

    /**
     * AlertEventDTO → AlertRecord 转换
     */
    private AlertRecord convertToRecord(AlertEventDTO event) {
        AlertRecord record = new AlertRecord();
        record.setRuleId(event.getRuleId());
        record.setRuleName(event.getRuleName());
        record.setLogicType(event.getLogicType());
        record.setRuleType(event.getRuleType());
        record.setPlantId(event.getPlantId());
        record.setStationId(event.getStationId());
        record.setDistrictCode(event.getDistrictCode());
        record.setLevel(event.getLevel());
        record.setContent(event.getContent());
        record.setMetric(event.getMetric());
        record.setMetricValue(event.getMetricValue());
        record.setThreshold(event.getThreshold());
        record.setTriggerTime(event.getTriggerTime() != null ? event.getTriggerTime() : LocalDateTime.now());
        record.setDataTime(event.getDataTime());
        record.setAggregationId(event.getAggregationId());
        record.setSuppressed(event.isSuppressed());
        record.setPushStatus(event.isSuppressed() ? "suppressed" : "pending");
        return record;
    }
}
