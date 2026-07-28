package com.huadianguangdong.analysis.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统计项 VO
 *
 * <p>通用的「标签 - 值 - 颜色」三元组，用于驾驶舱卡片 / 图例展示。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标签（如：正常 / 预警 / 危险） */
    private String label;

    /** 数值 */
    private Integer value;

    /** 颜色（hex 色值，如：#52c41a） */
    private String color;
}
