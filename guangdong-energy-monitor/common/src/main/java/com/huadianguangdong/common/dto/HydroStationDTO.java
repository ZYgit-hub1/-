package com.huadianguangdong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 水文站传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HydroStationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 水文站 ID */
    private Long id;

    /** 水文站名称 */
    private String name;

    /** 所属河流 */
    private String river;

    /** 所属城市 */
    private String city;

    /** 经度 */
    private double lng;

    /** 纬度 */
    private double lat;

    /** 警戒水位 */
    private double warningLevel;

    /** 保证水位 */
    private double guaranteeLevel;
}
