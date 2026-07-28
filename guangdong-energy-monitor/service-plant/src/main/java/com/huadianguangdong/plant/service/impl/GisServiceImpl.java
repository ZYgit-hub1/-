package com.huadianguangdong.plant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huadianguangdong.common.util.GeoUtil;
import com.huadianguangdong.plant.entity.HydroStation;
import com.huadianguangdong.plant.entity.Plant;
import com.huadianguangdong.plant.service.GisService;
import com.huadianguangdong.plant.service.HydroStationService;
import com.huadianguangdong.plant.service.PlantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GIS 空间服务实现
 *
 * @author huadianguangdong
 */
@Service
public class GisServiceImpl implements GisService {

    @Autowired
    private PlantService plantService;

    @Autowired
    private HydroStationService hydroStationService;

    @Override
    public List<Plant> findPlantsInBounds(double minLng, double maxLng, double minLat, double maxLat) {
        return plantService.list(new LambdaQueryWrapper<Plant>()
                        .isNotNull(Plant::getLng)
                        .isNotNull(Plant::getLat))
                .stream()
                .filter(p -> GeoUtil.isInBounds(p.getLng(), p.getLat(), minLng, maxLng, minLat, maxLat))
                .collect(Collectors.toList());
    }

    @Override
    public List<HydroStation> findHydroStationsInBounds(double minLng, double maxLng, double minLat, double maxLat) {
        return hydroStationService.list(new LambdaQueryWrapper<HydroStation>()
                        .isNotNull(HydroStation::getLng)
                        .isNotNull(HydroStation::getLat))
                .stream()
                .filter(h -> GeoUtil.isInBounds(h.getLng(), h.getLat(), minLng, maxLng, minLat, maxLat))
                .collect(Collectors.toList());
    }

    @Override
    public double calculateDistance(double lng1, double lat1, double lng2, double lat2) {
        return GeoUtil.distanceKm(lng1, lat1, lng2, lat2);
    }
}
