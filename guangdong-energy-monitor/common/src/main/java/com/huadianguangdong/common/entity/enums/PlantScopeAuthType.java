package com.huadianguangdong.common.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 电厂数据权限校验类型
 * <p>
 * 用于 {@code @PlantScopeAuth} 注解，指定接口需要的权限级别。
 *
 * @author huadianguangdong
 */
@Getter
@AllArgsConstructor
public enum PlantScopeAuthType {

    /** 仅校验登录，不限厂区 */
    LOGIN_ONLY(0, "仅登录"),

    /** 厂区级权限：PLANT_MANAGER 可看本厂+周边，OPERATOR 仅看本厂 */
    PLANT_SCOPE(1, "厂区权限"),

    /** 集团级权限：仅 ADMIN / PROD_SAFETY 可访问 */
    GROUP_ONLY(2, "集团权限");

    private final int level;
    private final String desc;
}
