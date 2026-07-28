package com.huadianguangdong.common.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 电厂类型枚举（对应 PostgreSQL power_plant_type）
 *
 * @author huadianguangdong
 */
public enum PowerPlantType {

    /** 燃煤电厂 */
    COAL("coal"),
    /** 燃气电厂 */
    GAS("gas"),
    /** 光伏电站 */
    SOLAR("solar"),
    /** 风电场 */
    WIND("wind"),
    /** 储能电站 */
    STORAGE("storage");

    @EnumValue
    private final String code;

    PowerPlantType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PowerPlantType of(String code) {
        for (PowerPlantType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知电厂类型: " + code);
    }
}
