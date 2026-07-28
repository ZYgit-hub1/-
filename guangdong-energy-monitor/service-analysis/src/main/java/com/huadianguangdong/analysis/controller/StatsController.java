package com.huadianguangdong.analysis.controller;

import com.huadianguangdong.analysis.service.StatsService;
import com.huadianguangdong.analysis.vo.DashboardVO;
import com.huadianguangdong.analysis.vo.PlantTrendVO;
import com.huadianguangdong.analysis.vo.StatItem;
import com.huadianguangdong.common.api.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计分析 Controller
 *
 * <p>对外暴露驾驶舱、电厂趋势、报警统计、水文统计等查询接口。
 *
 * @author huadianguangdong
 */
@Tag(name = "统计分析", description = "驾驶舱 / 趋势 / 报警 / 水文统计")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 驾驶舱汇总数据。
     *
     * @return Dashboard VO
     */
    @Operation(summary = "获取驾驶舱汇总数据")
    @GetMapping("/dashboard")
    public R<DashboardVO> dashboard() {
        return R.ok(statsService.getDashboard());
    }

    /**
     * 电厂指标趋势（折线图）。
     *
     * @param plantId   电厂 ID
     * @param metric    指标：waterLevel / power
     * @param startTime 起始时间（yyyy-MM-dd HH:mm:ss）
     * @param endTime   结束时间（yyyy-MM-dd HH:mm:ss）
     * @return 趋势 VO
     */
    @Operation(summary = "查询电厂指标趋势")
    @GetMapping("/plant-trend")
    public R<PlantTrendVO> plantTrend(
            @Parameter(description = "电厂 ID") @RequestParam Long plantId,
            @Parameter(description = "指标：waterLevel / power") @RequestParam String metric,
            @Parameter(description = "起始时间 yyyy-MM-dd HH:mm:ss") @RequestParam String startTime,
            @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss") @RequestParam String endTime) {
        return R.ok(statsService.getPlantTrend(plantId, metric, startTime, endTime));
    }

    /**
     * 按报警级别统计。
     *
     * @param startTime 起始时间
     * @param endTime   结束时间
     * @return 按级别统计的项列表
     */
    @Operation(summary = "按报警级别统计")
    @GetMapping("/alarm-statistics")
    public R<List<StatItem>> alarmStatistics(
            @Parameter(description = "起始时间 yyyy-MM-dd HH:mm:ss") @RequestParam String startTime,
            @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss") @RequestParam String endTime) {
        return R.ok(statsService.getAlarmStatistics(startTime, endTime));
    }

    /**
     * 水文站统计数据。
     *
     * @param stationId 水文站 ID
     * @param startTime 起始时间
     * @param endTime   结束时间
     * @return 水文站统计数据
     */
    @Operation(summary = "查询水文站统计数据")
    @GetMapping("/hydro-statistics")
    public R<List<Map<String, Object>>> hydroStatistics(
            @Parameter(description = "水文站 ID") @RequestParam Long stationId,
            @Parameter(description = "起始时间 yyyy-MM-dd HH:mm:ss") @RequestParam String startTime,
            @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss") @RequestParam String endTime) {
        return R.ok(statsService.getHydroStatistics(stationId, startTime, endTime));
    }
}
