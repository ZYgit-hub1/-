package com.huadianguangdong.plant.controller;

import com.huadianguangdong.common.api.R;
import com.huadianguangdong.plant.entity.HydroStation;
import com.huadianguangdong.plant.entity.Plant;
import com.huadianguangdong.plant.service.GisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GIS 空间服务 Controller
 *
 * @author huadianguangdong
 */
@Tag(name = "GIS 空间服务", description = "基于经纬度边界框的范围查询与距离计算")
@RestController
@RequestMapping("/api/gis")
public class GisController {

    @Autowired
    private GisService gisService;

    @Operation(summary = "查询边界范围内的电厂")
    @GetMapping("/plants/bounds")
    public R<List<Plant>> plantsInBounds(@RequestParam double minLng,
                                         @RequestParam double maxLng,
                                         @RequestParam double minLat,
                                         @RequestParam double maxLat) {
        return R.ok(gisService.findPlantsInBounds(minLng, maxLng, minLat, maxLat));
    }

    @Operation(summary = "查询边界范围内的水文站")
    @GetMapping("/hydro/stations/bounds")
    public R<List<HydroStation>> hydroStationsInBounds(@RequestParam double minLng,
                                                       @RequestParam double maxLng,
                                                       @RequestParam double minLat,
                                                       @RequestParam double maxLat) {
        return R.ok(gisService.findHydroStationsInBounds(minLng, maxLng, minLat, maxLat));
    }

    @Operation(summary = "计算两个经纬度坐标之间的距离（千米）")
    @GetMapping("/distance")
    public R<Double> distance(@RequestParam double lng1,
                              @RequestParam double lat1,
                              @RequestParam double lng2,
                              @RequestParam double lat2) {
        return R.ok(gisService.calculateDistance(lng1, lat1, lng2, lat2));
    }
}
