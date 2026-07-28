package com.huadianguangdong.common.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 气象预警类型枚举（对应 PostgreSQL weather_warning_type）
 *
 * @author huadianguangdong
 */
public enum WeatherWarningType {

    TYPHOON("typhoon", "台风"),
    RAINSTORM("rainstorm", "暴雨"),
    HIGH_TEMP("high_temp", "高温"),
    LOW_TEMP("low_temp", "低温"),
    GALE("gale", "大风"),
    FOG("fog", "大雾"),
    HAZE("haze", "霾"),
    THUNDER("thunder", "雷暴"),
    ICE("ice", "结冰"),
    DROUGHT("drought", "干旱"),
    FLOOD("flood", "洪水"),
    OTHER("other", "其他");

    @EnumValue
    private final String code;
    private final String label;

    WeatherWarningType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
