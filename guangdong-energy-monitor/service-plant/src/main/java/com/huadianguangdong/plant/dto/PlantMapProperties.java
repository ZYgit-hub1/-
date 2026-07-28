package com.huadianguangdong.plant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地图聚合电厂属性（GeoJSON properties）
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlantMapProperties {

    /** 电厂 ID */
    private Long plantId;

    /** 电厂名称 */
    private String name;

    /** 电厂类型：coal / gas / solar / wind / storage */
    private String type;

    /** 当前最高预警等级：blue / yellow / orange / red / null（无预警） */
    private String warningLevel;

    /** 最新温度（℃） */
    private Double latestTemp;
}
