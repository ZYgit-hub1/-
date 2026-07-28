package com.huadianguangdong.plant.dto;

import lombok.Data;

/**
 * 电厂地图查询行数据（Mapper 层返回）
 * <p>
 * 对应 PostGIS 空间查询 + 气象预警关联的 SQL 结果集。
 *
 * @author huadianguangdong
 */
@Data
public class PlantMapRow {

    /** 电厂 ID */
    private Long plantId;

    /** 电厂名称 */
    private String name;

    /** 电厂类型 */
    private String type;

    /** 经度 */
    private Double lng;

    /** 纬度 */
    private Double lat;

    /** 最高预警等级（blue / yellow / orange / red / null） */
    private String warningLevel;

    /** 最新温度（℃） */
    private Double latestTemp;
}
