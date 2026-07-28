package com.huadianguangdong.analysis.service;

import com.huadianguangdong.analysis.vo.DashboardVO;
import com.huadianguangdong.analysis.vo.PlantTrendVO;
import com.huadianguangdong.analysis.vo.StatItem;

import java.util.List;

/**
 * 统计分析服务
 *
 * <p>面向驾驶舱与报表场景，提供电厂 / 报警 / 水文等维度的聚合统计能力。
 *
 * @author huadianguangdong
 */
public interface StatsService {

    /**
     * 获取驾驶舱汇总数据。
     *
     * <p>结果缓存 5 分钟。
     *
     * @return 驾驶舱统计 VO
     */
    DashboardVO getDashboard();

    /**
     * 查询电厂指标趋势（折线图）。
     *
     * @param plantId   电厂 ID
     * @param metric    指标：waterLevel / power
     * @param startTime 起始时间（yyyy-MM-dd HH:mm:ss）
     * @param endTime   结束时间（yyyy-MM-dd HH:mm:ss）
     * @return 趋势 VO
     */
    PlantTrendVO getPlantTrend(Long plantId, String metric, String startTime, String endTime);

    /**
     * 按报警级别统计。
     *
     * @param startTime 起始时间
     * @param endTime   结束时间
     * @return 按级别统计的项列表
     */
    List<StatItem> getAlarmStatistics(String startTime, String endTime);

    /**
     * 查询水文站统计数据。
     *
     * @param stationId 水文站 ID
     * @param startTime 起始时间
     * @param endTime   结束时间
     * @return 水文站统计数据（time_point -> water_level / flow_rate / ...）
     */
    List<java.util.Map<String, Object>> getHydroStatistics(Long stationId, String startTime, String endTime);
}
