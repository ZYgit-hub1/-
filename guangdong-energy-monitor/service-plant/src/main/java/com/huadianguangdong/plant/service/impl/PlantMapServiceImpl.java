package com.huadianguangdong.plant.service.impl;

import com.huadianguangdong.plant.dto.PlantMapCollection;
import com.huadianguangdong.plant.dto.PlantMapFeature;
import com.huadianguangdong.plant.dto.PlantMapProperties;
import com.huadianguangdong.plant.dto.PlantMapRow;
import com.huadianguangdong.plant.mapper.PlantMapMapper;
import com.huadianguangdong.plant.service.PlantMapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 电厂地图聚合服务实现
 * <p>
 * 核心流程：
 * 1. 接收前端传入的 bounds（minLng, minLat, maxLng, maxLat）
 * 2. 调用 PlantMapMapper 执行 PostGIS 空间查询 + 预警关联
 * 3. 将查询结果转换为标准 GeoJSON FeatureCollection
 * 4. 使用 @Cacheable 缓存结果，以 bounds 字符串为 key
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
public class PlantMapServiceImpl implements PlantMapService {

    @Autowired
    private PlantMapMapper plantMapMapper;

    /**
     * 查询可视边界范围内的电厂，返回 GeoJSON FeatureCollection
     * <p>
     * 缓存策略：
     * - cacheName: plant_map
     * - key: 由 bounds 四个参数拼接而成，确保不同视野范围缓存隔离
     * - 建议在 Redis 中配置 TTL（如 60s），避免预警状态更新不及时
     *
     * @param minLng 最小经度
     * @param minLat 最小纬度
     * @param maxLng 最大经度
     * @param maxLat 最大纬度
     * @return GeoJSON FeatureCollection
     */
    @Override
    @Cacheable(value = "plant_map", key = "#minLng + ',' + #minLat + ',' + #maxLng + ',' + #maxLat")
    public PlantMapCollection getPlantsInBounds(double minLng, double minLat, double maxLng, double maxLat) {
        log.debug("查询地图聚合电厂 bounds=[{},{},{},{}]", minLng, minLat, maxLng, maxLat);

        List<PlantMapRow> rows = plantMapMapper.selectPlantsInBounds(minLng, minLat, maxLng, maxLat);
        List<PlantMapFeature> features = new ArrayList<>(rows.size());

        for (PlantMapRow row : rows) {
            PlantMapFeature feature = buildFeature(row);
            features.add(feature);
        }

        return PlantMapCollection.builder()
                .type("FeatureCollection")
                .features(features)
                .build();
    }

    /**
     * 将 Mapper 查询行转换为 GeoJSON Feature
     */
    private PlantMapFeature buildFeature(PlantMapRow row) {
        PlantMapProperties properties = PlantMapProperties.builder()
                .plantId(row.getPlantId())
                .name(row.getName())
                .type(row.getType())
                .warningLevel(row.getWarningLevel())
                .latestTemp(row.getLatestTemp())
                .build();

        PlantMapFeature.GeoJsonGeometry geometry = PlantMapFeature.GeoJsonGeometry.builder()
                .type("Point")
                .coordinates(Arrays.asList(row.getLng(), row.getLat()))
                .build();

        return PlantMapFeature.builder()
                .type("Feature")
                .geometry(geometry)
                .properties(properties)
                .build();
    }
}
