package com.huadianguangdong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 水文数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HydroDataDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 水文站 ID */
    private Long stationId;

    /** 水位 */
    private double waterLevel;

    /** 流量 */
    private double flowRate;

    /** 水位趋势（up/down/stable） */
    private String trend;

    /** 预警级别 */
    private String alertLevel;

    /** 采集时间 */
    private String readingTime;
}
