package com.huadianguangdong.common.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 报警规则逻辑类型枚举
 * <p>
 * 描述规则匹配引擎的执行逻辑，与业务域 {@link AlertRuleType} 正交：
 * <ul>
 *   <li>{@link #STATIC_THRESHOLD} —— 静态阈值：单一指标超过固定阈值即触发</li>
 *   <li>{@link #DYNAMIC_DEVIATION} —— 动态偏离：当前值偏离滑动均值超过 N 个标准差即触发</li>
 *   <li>{@link #COMBO_LOGIC} —— 组合逻辑：多指标同时满足条件才触发（如高温+低湿+强风）</li>
 *   <li>{@link #TREND_WARNING} —— 趋势预警：连续 N 个采样点单调上升/下降即触发</li>
 * </ul>
 *
 * @author huadianguangdong
 */
public enum RuleLogicType {

    /** 静态阈值：指标 op threshold */
    STATIC_THRESHOLD("static_threshold"),

    /** 动态偏离：|value - avg| > n * stddev */
    DYNAMIC_DEVIATION("dynamic_deviation"),

    /** 组合逻辑：多指标布尔组合 */
    COMBO_LOGIC("combo_logic"),

    /** 趋势预警：连续 N 点同向变化 */
    TREND_WARNING("trend_warning");

    @EnumValue
    private final String code;

    RuleLogicType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
