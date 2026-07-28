package com.huadianguangdong.collector.weather;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 气象数据清洗阈值配置（可动态调整）
 * <p>
 * 通过 application.yml 的 {@code collector.weather.cleaning} 前缀注入，
 * 突破硬编码限制，可在不同环境（生产/测试）或季节（如夏季调高温度上限）灵活配置。
 *
 * @author huadianguangdong
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "collector.weather.cleaning")
public class WeatherCleaningProperties {

    /** 温度下限（℃），低于此值标记 null */
    private float tempMin = -40.0f;

    /** 温度上限（℃），高于此值标记 null（默认 60℃） */
    private float tempMax = 60.0f;

    /** 湿度下限（%） */
    private float humidityMin = 0.0f;

    /** 湿度上限（%） */
    private float humidityMax = 100.0f;

    /** 风速下限（m/s） */
    private float windSpeedMin = 0.0f;

    /** 风速上限（m/s），17 级风上限 */
    private float windSpeedMax = 62.0f;

    /** 风向下限（°） */
    private short windDirMin = 0;

    /** 风向上限（°） */
    private short windDirMax = 359;

    /** 降雨量下限（mm） */
    private float rainMin = 0.0f;

    /** 降雨量上限（mm），单小时极端降雨上限 */
    private float rainMax = 500.0f;

    /** 气压下限（hPa） */
    private float pressureMin = 800.0f;

    /** 气压上限（hPa） */
    private float pressureMax = 1100.0f;

    /** 是否启用清洗（false 时保留原始值不清洗） */
    private boolean enabled = true;
}
