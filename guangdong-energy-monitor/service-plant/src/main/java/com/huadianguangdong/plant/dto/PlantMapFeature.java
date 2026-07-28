package com.huadianguangdong.plant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GeoJSON Feature —— 电厂地图聚合要素
 * <p>
 * 符合 RFC 7946 GeoJSON 规范，type 固定为 "Feature"，
 * geometry 为 Point，properties 包含电厂业务字段。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlantMapFeature {

    /** GeoJSON 类型，固定 Feature */
    private String type = "Feature";

    /** 几何信息（Point） */
    private GeoJsonGeometry geometry;

    /** 电厂业务属性 */
    private PlantMapProperties properties;

    /**
     * GeoJSON Geometry（Point）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GeoJsonGeometry {

        /** GeoJSON 几何类型，固定 Point */
        private String type = "Point";

        /** 坐标数组 [lng, lat] */
        private List<Double> coordinates;
    }
}
