package com.huadianguangdong.analysis.job;

import com.huadianguangdong.analysis.entity.DailyStats;
import com.huadianguangdong.analysis.mapper.DailyStatsMapper;
import com.huadianguangdong.analysis.mapper.StatsMapper;
import com.huadianguangdong.analysis.service.StatsService;
import com.huadianguangdong.analysis.vo.DashboardVO;
import com.huadianguangdong.common.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 统计定时任务
 *
 * <p>每小时执行一次：计算当日统计指标 -> 写入日报表（daily_stats）-> 刷新 Redis 缓存。
 * <ul>
 *     <li>聚合电厂状态、报警数、水文站告警数等指标</li>
 *     <li>落库当日日报表（存在则更新）</li>
 *     <li>清空 Dashboard 缓存，下次查询触发重算</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsScheduleJob {

    private final StatsService statsService;
    private final StatsMapper statsMapper;
    private final DailyStatsMapper dailyStatsMapper;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    /** Dashboard 缓存 key（与 StatsServiceImpl 保持一致） */
    private static final String CACHE_KEY_DASHBOARD = "analysis:dashboard";

    /**
     * 每小时整点执行：计算日统计并写入日报表 + Redis。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void computeDailyStats() {
        log.info("定时统计任务开始: {}", LocalDateTime.now());
        try {
            DashboardVO dashboard = statsService.getDashboard();
            LocalDate today = LocalDate.now();

            DailyStats stats = buildDailyStats(dashboard, today);
            // 简化：每日覆盖插入；实际可按 (statsDate) 唯一约束 upsert
            dailyStatsMapper.insert(stats);
            log.info("日报表已写入: statsDate={}, totalPlants={}, totalAlarms={}",
                    today, stats.getTotalPlants(), stats.getTotalAlarms());

            // 刷新 Redis：清空 Dashboard 缓存，下次查询触发重算并写回
            redisUtil.del(CACHE_KEY_DASHBOARD);

            // 写入小时级统计快照（便于趋势展示）
            String snapshotKey = "analysis:snapshot:" + today + ":" + LocalDateTime.now().getHour();
            redisUtil.setEx(snapshotKey, objectMapper.writeValueAsString(dashboard), 7, TimeUnit.DAYS);
            log.info("小时级统计快照已写入: {}", snapshotKey);
        } catch (Exception e) {
            log.error("定时统计任务执行失败", e);
        }
    }

    /**
     * 组装日报表实体。
     */
    private DailyStats buildDailyStats(DashboardVO dashboard, LocalDate today) {
        DailyStats stats = new DailyStats();
        stats.setStatsDate(today);
        stats.setTotalPlants(dashboard.getTotalPlants());
        stats.setNormalCount(dashboard.getNormalCount());
        stats.setWarningCount(dashboard.getWarningCount());
        stats.setDangerCount(dashboard.getDangerCount());
        stats.setOfflineCount(dashboard.getOfflineCount());
        stats.setTotalAlarms(dashboard.getTotalAlarms());
        stats.setActiveWarnings(dashboard.getActiveWarnings());
        stats.setHydroStationCount(dashboard.getHydroStationCount());
        stats.setHydroAlertCount(dashboard.getHydroAlertCount());
        stats.setCreateTime(LocalDateTime.now());
        return stats;
    }
}
