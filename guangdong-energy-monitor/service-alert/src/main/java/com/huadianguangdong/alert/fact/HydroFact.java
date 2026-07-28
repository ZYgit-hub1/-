package com.huadianguangdong.alert.fact;

import com.huadianguangdong.common.dto.HydroDataDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 水文事实对象
 * <p>
 * 封装 {@link HydroDataDTO} 及其阈值参数，作为 Drools 规则的事实（Fact）插入 KieSession。<br>
 * Drools 通过字段访问（getter）匹配 LHS 条件。
 *
 * @author huadianguangdong
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HydroFact implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 原始水文数据 */
    private HydroDataDTO hydroData;

    /** 警戒水位 */
    private double warningLevel;

    /** 保证水位（防洪高水位） */
    private double guaranteeLevel;

    /**
     * 便捷访问：当前水位
     */
    public double getWaterLevel() {
        return hydroData == null ? 0 : hydroData.getWaterLevel();
    }

    /**
     * 便捷访问：水位趋势
     */
    public String getTrend() {
        return hydroData == null ? null : hydroData.getTrend();
    }

    /**
     * 便捷访问：预警级别
     */
    public String getAlertLevel() {
        return hydroData == null ? null : hydroData.getAlertLevel();
    }
}
