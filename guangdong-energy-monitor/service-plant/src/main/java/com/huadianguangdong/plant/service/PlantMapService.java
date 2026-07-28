package com.huadianguangdong.plant.service;

import com.huadianguangdong.plant.dto.PlantMapCollection;

/**
 * 电厂地图聚合服务接口
 * <p>
 * 提供地图可视区域电厂查询能力，返回标准 GeoJSON FeatureCollection，
 * 每个要素包含电厂基础信息、最高预警等级和最新温度。
 *
 * @author huadianguangdong
 */
public interface PlantMapService {

    /**
     * 查询可视边界范围内的电厂，返回 GeoJSON FeatureCollection
     * <p>
     * 使用 PostGIS 空间查询，关联 t_weather_warning 计算每个电厂当前最高预警等级。
     * 结果缓存在 Redis，以 bounds 为 key，减少数据库压力。
     *
     * @param minLng 最小经度
     * @param minLat 最小纬度
     * @param maxLng 最大经度
     * @param maxLat 最大纬度
     * @return GeoJSON FeatureCollection
     */
    PlantMapCollection getPlantsInBounds(double minLng, double minLat, double maxLng, double maxLat);
}
