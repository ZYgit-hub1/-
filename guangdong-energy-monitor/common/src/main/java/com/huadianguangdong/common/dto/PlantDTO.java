package com.huadianguangdong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 电厂传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 电厂 ID */
    private Long id;

    /** 电厂名称 */
    private String name;

    /** 电厂类型 */
    private String type;

    /** 经度 */
    private double lng;

    /** 纬度 */
    private double lat;

    /** 装机容量 */
    private int capacity;

    /** 运行状态 */
    private String status;

    /** 地址 */
    private String address;
}
