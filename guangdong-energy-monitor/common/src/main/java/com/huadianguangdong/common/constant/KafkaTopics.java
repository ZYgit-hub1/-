package com.huadianguangdong.common.constant;

/**
 * Kafka Topic 常量
 */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    /** 报警 Topic */
    public static final String ALARM = "alarm-topic";

    /** 水文数据 Topic */
    public static final String HYDRO_DATA = "hydro-data-topic";

    /** 气象数据 Topic（业务消费，兼容旧链路） */
    public static final String WEATHER_DATA = "weather-data-topic";

    /** 气象原始数据 Topic（采集服务 → 报警服务 / TDengine / 分析服务） */
    public static final String WEATHER_RAW = "weather.raw";

    /** 水文实时数据 Topic（采集服务 → 报警服务 / 分析服务） */
    public static final String HYDRO_LEVEL = "hydro_level";

    /** 电厂状态 Topic */
    public static final String PLANT_STATUS = "plant-status-topic";

    /** 报警事件 Topic（报警服务 → 推送服务 / 分析服务） */
    public static final String ALERT_EVENT = "alert.event";

    /** 趋势预警 Topic（预测服务 → 报警服务，用于 TREND_WARNING 规则触发） */
    public static final String TREND_WARNING = "trend.warning";
}
