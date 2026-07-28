package com.huadianguangdong.plant.service;

import com.huadianguangdong.plant.entity.HydroStation;
import com.huadianguangdong.plant.entity.Plant;

import java.util.List;

/**
 * GIS 空间服务接口
 * <p>
 * 提供基于经纬度边界框的范围查询与距离计算能力。
 *
 * @author huadianguangdong
 */
public interface GisService {

    /**
     * 查询落在指定经纬度边界范围内的电厂
     *
     * @param minLng 最小经度
     * @param maxLng 最大经度
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @return 电厂列表
     */
    List<Plant> findPlantsInBounds(double minLng, double maxLng, double minLat, double maxLat);

    /**
     * 查询落在指定经纬度边界范围内的水文站
     *
     * @param minLng 最小经度
     * @param maxLng 最大经度
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @return 水文站列表
     */
    List<HydroStation> findHydroStationsInBounds(double minLng, double maxLng, double minLat, double maxLat);

    /**
     * 计算两个经纬度坐标之间的球面距离（千米）
     *
     * @param lng1 经度 1
     * @param lat1 纬度 1
     * @param lng2 经度 2
     * @param lat2 纬度 2
     * @return 距离（千米）
     */
    double calculateDistance(double lng1, double lat1, double lng2, double lat2);
}
