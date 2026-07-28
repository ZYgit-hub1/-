package com.huadianguangdong.common.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 运行状态枚举（对应 PostgreSQL plant_status）
 * 适用于电厂、水文站等实体的运行状态。
 *
 * @author huadianguangdong
 */
public enum PlantStatus {

    /** 正常 */
    NORMAL("normal"),
    /** 预警 */
    WARNING("warning"),
    /** 报警 */
    DANGER("danger"),
    /** 离线 */
    OFFLINE("offline");

    @EnumValue
    private final String code;

    PlantStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PlantStatus of(String code) {
        for (PlantStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知运行状态: " + code);
    }
}
