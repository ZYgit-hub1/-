package com.huadianguangdong.common.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 预警级别枚举（对应 PostgreSQL warning_level）
 * 对应国家突发气象灾害预警信号发布标准。
 *
 * @author huadianguangdong
 */
public enum WarningLevel {

    /** 蓝色预警（IV级，最低） */
    BLUE("blue", "蓝色", 4),
    /** 黄色预警（III级） */
    YELLOW("yellow", "黄色", 3),
    /** 橙色预警（II级） */
    ORANGE("orange", "橙色", 2),
    /** 红色预警（I级，最高） */
    RED("red", "红色", 1);

    @EnumValue
    private final String code;
    private final String label;
    /** 严重程度序号（1 最严重） */
    private final int severity;

    WarningLevel(String code, String label, int severity) {
        this.code = code;
        this.label = label;
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public int getSeverity() {
        return severity;
    }
}
