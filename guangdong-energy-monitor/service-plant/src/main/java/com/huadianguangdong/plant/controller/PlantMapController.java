package com.huadianguangdong.plant.controller;

import com.huadianguangdong.common.api.R;
import com.huadianguangdong.common.exception.BusinessException;
import com.huadianguangdong.plant.dto.PlantMapCollection;
import com.huadianguangdong.plant.service.PlantMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 电厂地图聚合接口
 * <p>
 * 供前端大屏/地图组件调用，传入可视区域 bounds，
 * 返回 GeoJSON FeatureCollection，可直接对接 Leaflet / Mapbox / AMap。
 *
 * @author huadianguangdong
 */
@Tag(name = "电厂地图聚合", description = "地图可视区域电厂查询（PostGIS + 预警关联 + Redis 缓存）")
@RestController
@RequestMapping("/api/plant-map")
public class PlantMapController {

    @Autowired
    private PlantMapService plantMapService;

    /**
     * 查询可视边界范围内的电厂（GeoJSON）
     *
     * @param minLng 最小经度（左下角）
     * @param minLat 最小纬度（左下角）
     * @param maxLng 最大经度（右上角）
     * @param maxLat 最大纬度（右上角）
     * @return GeoJSON FeatureCollection
     */
    @Operation(summary = "查询可视区域电厂（GeoJSON）",
            description = "使用 PostGIS 空间查询，关联气象预警，返回 GeoJSON FeatureCollection")
    @GetMapping
    public R<PlantMapCollection> getPlantsInBounds(
            @Parameter(description = "最小经度", example = "109.0", required = true)
            @RequestParam double minLng,
            @Parameter(description = "最小纬度", example = "20.0", required = true)
            @RequestParam double minLat,
            @Parameter(description = "最大经度", example = "117.5", required = true)
            @RequestParam double maxLng,
            @Parameter(description = "最大纬度", example = "25.5", required = true)
            @RequestParam double maxLat) {

        // 参数校验
        if (minLng >= maxLng || minLat >= maxLat) {
            throw new BusinessException("bounds 参数无效：minLng 必须 < maxLng，minLat 必须 < maxLat");
        }
        // 经纬度合理范围校验（中国范围）
        if (minLng < 73 || maxLng > 136 || minLat < 3 || maxLat > 54) {
            throw new BusinessException("bounds 经纬度超出合理范围");
        }

        PlantMapCollection result = plantMapService.getPlantsInBounds(minLng, minLat, maxLng, maxLat);
        return R.ok(result);
    }
}
