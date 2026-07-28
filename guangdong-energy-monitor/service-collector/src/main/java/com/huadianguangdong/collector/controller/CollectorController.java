package com.huadianguangdong.collector.controller;

import com.huadianguangdong.collector.entity.EmergencyEvent;
import com.huadianguangdong.collector.entity.HydroData;
import com.huadianguangdong.collector.entity.WeatherData;
import com.huadianguangdong.collector.service.EmergencyCollectService;
import com.huadianguangdong.collector.service.HydroCollectService;
import com.huadianguangdong.collector.service.WeatherCollectService;
import com.huadianguangdong.common.api.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据采集 Controller
 * <p>
 * 提供气象 / 水文 / 应急数据的手动触发采集与查询接口。
 *
 * @author huadianguangdong
 */
@Tag(name = "数据采集", description = "气象/水文/应急数据采集与查询")
@RestController
@RequestMapping("/api/collector")
public class CollectorController {

    @Autowired
    private WeatherCollectService weatherCollectService;

    @Autowired
    private HydroCollectService hydroCollectService;

    @Autowired
    private EmergencyCollectService emergencyCollectService;

    @Operation(summary = "手动触发气象采集")
    @GetMapping("/weather/trigger")
    public R<List<WeatherData>> triggerWeather() {
        return R.ok(weatherCollectService.collectAndPush());
    }

    @Operation(summary = "手动触发水文采集")
    @GetMapping("/hydro/trigger")
    public R<List<HydroData>> triggerHydro() {
        return R.ok(hydroCollectService.collectAndPush());
    }

    @Operation(summary = "手动触发应急事件采集")
    @GetMapping("/emergency/trigger")
    public R<List<EmergencyEvent>> triggerEmergency() {
        return R.ok(emergencyCollectService.collectAndPush());
    }

    @Operation(summary = "查询电厂最新气象数据")
    @GetMapping("/weather/{plantId}")
    public R<WeatherData> getLatestWeather(@PathVariable Long plantId) {
        WeatherData weatherData = weatherCollectService.getLatestByPlantId(plantId);
        return R.ok(weatherData);
    }

    @Operation(summary = "查询水文站历史数据")
    @GetMapping("/hydro/{stationId}/readings")
    public R<List<HydroData>> listHydroReadings(
            @PathVariable Long stationId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(hydroCollectService.listReadings(stationId, startTime, endTime));
    }
}
