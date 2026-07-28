package com.huadianguangdong.common.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 报警规则类型枚举（对应 PostgreSQL alert_rule_type）
 *
 * @author huadianguangdong
 */
public enum AlertRuleType {

    /** 水文规则 */
    HYDRO("hydro"),
    /** 气象规则 */
    WEATHER("weather"),
    /** 火情规则 */
    FIRE("fire"),
    /** 设备规则 */
    EQUIPMENT("equipment"),
    /** 组合规则 */
    COMPOSITE("composite");

    @EnumValue
    private final String code;

    AlertRuleType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
