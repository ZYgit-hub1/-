package com.huadianguangdong.alert.service.impl;

import com.huadianguangdong.alert.entity.Alarm;
import com.huadianguangdong.alert.entity.Warning;
import com.huadianguangdong.alert.notifier.Notifier;
import com.huadianguangdong.alert.service.AlertActionService;
import com.huadianguangdong.alert.service.AlarmService;
import com.huadianguangdong.alert.service.WarningService;
import com.huadianguangdong.common.constant.CommonConstants;
import com.huadianguangdong.common.dto.HydroDataDTO;
import com.huadianguangdong.common.dto.WeatherDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 规则动作服务实现
 * <p>
 * Drools 规则触发后回调本类，将报警 / 预警持久化并推送 Kafka，报警同时触发多通道通知。
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
public class AlertActionServiceImpl implements AlertActionService {

    /** 水文数据时间格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private WarningService warningService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired(required = false)
    private List<Notifier> notifiers;

    @Override
    public void createAlarm(HydroDataDTO hydroData, String reason) {
        log.warn("【规则触发-报警】stationId={}, waterLevel={}, reason={}",
                hydroData.getStationId(), hydroData.getWaterLevel(), reason);

        Alarm alarm = new Alarm();
        alarm.setLevel(determineAlarmLevel(hydroData));
        alarm.setStatus("unconfirmed");
        alarm.setContent(buildAlarmContent(hydroData, reason));
        alarm.setPlantId(null);
        alarm.setPlantName(null);
        alarm.setTriggerTime(parseTime(hydroData.getReadingTime()));

        // 持久化
        alarmService.save(alarm);

        // Kafka 推送
        try {
            kafkaTemplate.send(CommonConstants.TOPIC_ALARM, alarm);
        } catch (Exception e) {
            log.error("报警 Kafka 推送失败 alarmId={}", alarm.getId(), e);
        }

        // 多通道通知
        pushNotification(alarm);
    }

    @Override
    public void createWarning(HydroDataDTO hydroData, String reason) {
        log.info("【规则触发-预警】stationId={}, waterLevel={}, reason={}",
                hydroData.getStationId(), hydroData.getWaterLevel(), reason);

        Warning warning = new Warning();
        warning.setLevel(determineWarningLevel(hydroData));
        warning.setType("flood");
        warning.setContent(buildAlarmContent(hydroData, reason));
        warning.setPlantId(null);
        warning.setPlantName(null);
        warning.setStartTime(parseTime(hydroData.getReadingTime()));
        warning.setStatus("active");

        warningService.save(warning);
    }

    @Override
    public void createWeatherWarning(WeatherDataDTO weatherData, String reason) {
        log.info("【规则触发-气象预警】plantId={}, reason={}", weatherData.getPlantId(), reason);

        Warning warning = new Warning();
        warning.setLevel("yellow");
        warning.setType("weather");
        warning.setContent(reason);
        warning.setPlantId(weatherData.getPlantId());
        warning.setPlantName(null);
        warning.setStartTime(LocalDateTime.now());
        warning.setStatus("active");

        warningService.save(warning);
    }

    /**
     * 多通道推送通知
     */
    private void pushNotification(Alarm alarm) {
        if (notifiers == null || notifiers.isEmpty()) {
            return;
        }
        for (Notifier notifier : notifiers) {
            try {
                notifier.notify(alarm);
            } catch (Exception e) {
                log.error("通知通道推送失败: {} alarmId={}", notifier.getClass().getSimpleName(), alarm.getId(), e);
            }
        }
    }

    /**
     * 根据 alertLevel 判定报警级别
     */
    private String determineAlarmLevel(HydroDataDTO hydroData) {
        String alertLevel = hydroData.getAlertLevel();
        if ("flood".equals(alertLevel)) {
            return "emergency";
        } else if ("warning".equals(alertLevel)) {
            return "high";
        }
        return "medium";
    }

    /**
     * 根据水位趋势判定预警级别
     */
    private String determineWarningLevel(HydroDataDTO hydroData) {
        if (hydroData.getWaterLevel() > 15) {
            return "red";
        } else if (hydroData.getWaterLevel() > 12) {
            return "orange";
        }
        return "yellow";
    }

    /**
     * 构造报警内容
     */
    private String buildAlarmContent(HydroDataDTO hydroData, String reason) {
        return String.format("水文站[%d] %s，当前水位 %.2f m，流量 %.2f m³/s，趋势 %s",
                hydroData.getStationId(),
                reason,
                hydroData.getWaterLevel(),
                hydroData.getFlowRate(),
                hydroData.getTrend());
    }

    /**
     * 解析时间字符串
     */
    private LocalDateTime parseTime(String readingTime) {
        if (readingTime == null || readingTime.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(readingTime, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("时间解析失败，使用当前时间: {}", readingTime);
            return LocalDateTime.now();
        }
    }
}
