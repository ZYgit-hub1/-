package com.huadianguangdong.common.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 * <p>
 * 对应系统中的四级权限体系：
 * <ul>
 *   <li>ADMIN - 集团管理员：全局数据访问权限</li>
 *   <li>PROD_SAFETY - 生产安全主管：跨厂安全数据权限</li>
 *   <li>PLANT_MANAGER - 电厂经理：本厂数据 + 周边水文站权限</li>
 *   <li>OPERATOR - 运行操作员：仅本厂数据权限</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Getter
@AllArgsConstructor
public enum UserRole {

    /** 集团管理员：可查看所有电厂/水文站数据 */
    ADMIN("ADMIN", "集团管理员"),

    /** 生产安全主管：可查看多个电厂的安全告警 + 水文数据 */
    PROD_SAFETY("PROD_SAFETY", "生产安全主管"),

    /** 电厂经理：可查看本厂 + 周边水文站 */
    PLANT_MANAGER("PLANT_MANAGER", "电厂经理"),

    /** 运行操作员：仅查看本厂数据 */
    OPERATOR("OPERATOR", "运行操作员");

    /** 角色编码 */
    private final String code;

    /** 角色描述 */
    private final String desc;

    /**
     * 根据 code 获取枚举，未匹配时返回 null
     */
    public static UserRole fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (UserRole role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 判断是否具有集团级（全局）数据权限
     */
    public boolean isGlobalScope() {
        return this == ADMIN || this == PROD_SAFETY;
    }

    /**
     * 判断是否为电厂级权限（需校验 plant_scope）
     */
    public boolean isPlantScoped() {
        return this == PLANT_MANAGER || this == OPERATOR;
    }
}
