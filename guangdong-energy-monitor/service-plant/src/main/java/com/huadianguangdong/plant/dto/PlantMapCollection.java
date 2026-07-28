package com.huadianguangdong.plant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GeoJSON FeatureCollection —— 地图聚合响应
 * <p>
 * 符合 RFC 7946 GeoJSON 规范，前端可直接对接 Leaflet / Mapbox / AMap 等。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlantMapCollection {

    /** GeoJSON 类型，固定 FeatureCollection */
    private String type = "FeatureCollection";

    /** 电厂要素列表 */
    private List<PlantMapFeature> features;
}
