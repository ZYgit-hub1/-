package com.huadianguangdong.analysis.service.impl;

import com.huadianguangdong.analysis.mapper.StatsMapper;
import com.huadianguangdong.analysis.service.StatsService;
import com.huadianguangdong.analysis.vo.DashboardVO;
import com.huadianguangdong.analysis.vo.PlantTrendVO;
import com.huadianguangdong.analysis.vo.StatItem;
import com.huadianguangdong.common.util.RedisUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 统计分析服务实现
 *
 * <p>使用 MyBatis-Plus（StatsMapper）进行聚合查询，结果通过 {@link RedisUtil}
 * 缓存 5 分钟，降低 DB 压力。缓存采用 JSON 序列化。
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    /** 统计缓存 TTL（分钟），默认 5 分钟 */
    @Value("${analysis.cache.ttl-minutes:5}")
    private long cacheTtlMinutes;

    private static final String CACHE_KEY_DASHBOARD = "analysis:dashboard";
    private static final String CACHE_KEY_TREND_PREFIX = "analysis:trend:";
    private static final String CACHE_KEY_ALARM_PREFIX = "analysis:alarm:";
    private static final String CACHE_KEY_HYDRO_PREFIX = "analysis:hydro:";

    @Override
    public DashboardVO getDashboard() {
        String cacheKey = CACHE_KEY_DASHBOARD;
        try {
            String cached = redisUtil.get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, DashboardVO.class);
            }
        } catch (Exception e) {
            log.warn("读取 Dashboard 缓存失败，回源查询: {}", e.getMessage());
        }

        log.debug("回源查询 Dashboard 统计数据");
        Integer totalPlants = statsMapper.countPlant();
        Integer normalCount = 0;
        Integer warningCount = 0;
        Integer dangerCount = 0;
        Integer offlineCount = 0;
        List<Map<String, Object>> statusRows = statsMapper.countPlantByStatus();
        if (statusRows != null) {
            for (Map<String, Object> row : statusRows) {
                String status = String.valueOf(row.get("status"));
                int cnt = ((Number) row.get("cnt")).intValue();
                switch (status) {
                    case "NORMAL" -> normalCount = cnt;
                    case "WARNING" -> warningCount = cnt;
                    case "DANGER" -> dangerCount = cnt;
                    case "OFFLINE" -> offlineCount = cnt;
                    default -> log.debug("未知电厂状态: {}", status);
                }
            }
        }

        Integer hydroStationCount = statsMapper.countHydroStation();
        Integer hydroAlertCount = statsMapper.countHydroAlert();
        Integer activeWarnings = statsMapper.countActiveWarning();

        // 报警总数取近 24 小时
        java.time.LocalDateTime end = java.time.LocalDateTime.now();
        java.time.LocalDateTime start = end.minusHours(24);
        String startStr = start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String endStr = end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Integer totalAlarms = statsMapper.countAlarm(startStr, endStr);

        DashboardVO vo = DashboardVO.builder()
                .totalPlants(totalPlants == null ? 0 : totalPlants)
                .normalCount(normalCount)
                .warningCount(warningCount)
                .dangerCount(dangerCount)
                .offlineCount(offlineCount)
                .totalAlarms(totalAlarms == null ? 0 : totalAlarms)
                .activeWarnings(activeWarnings == null ? 0 : activeWarnings)
                .hydroStationCount(hydroStationCount == null ? 0 : hydroStationCount)
                .hydroAlertCount(hydroAlertCount == null ? 0 : hydroAlertCount)
                .build();

        try {
            redisUtil.setEx(cacheKey, objectMapper.writeValueAsString(vo), cacheTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入 Dashboard 缓存失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public PlantTrendVO getPlantTrend(Long plantId, String metric, String startTime, String endTime) {
        String cacheKey = CACHE_KEY_TREND_PREFIX + plantId + ":" + metric + ":" + startTime + ":" + endTime;
        try {
            String cached = redisUtil.get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, PlantTrendVO.class);
            }
        } catch (Exception e) {
            log.warn("读取 PlantTrend 缓存失败，回源查询: {}", e.getMessage());
        }

        List<Map<String, Object>> rows = statsMapper.selectPlantTrend(plantId, metric, startTime, endTime);
        List<String> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                dates.add(String.valueOf(row.get("time_point")));
                Object v = row.get("metric_value");
                values.add(v == null ? 0.0 : ((Number) v).doubleValue());
            }
        }
        String plantName = statsMapper.selectPlantName(plantId);
        PlantTrendVO vo = PlantTrendVO.builder()
                .plantId(plantId)
                .plantName(plantName)
                .dates(dates)
                .values(values)
                .metric(metric)
                .build();

        try {
            redisUtil.setEx(cacheKey, objectMapper.writeValueAsString(vo), cacheTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入 PlantTrend 缓存失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public List<StatItem> getAlarmStatistics(String startTime, String endTime) {
        String cacheKey = CACHE_KEY_ALARM_PREFIX + startTime + ":" + endTime;
        try {
            String cached = redisUtil.get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<StatItem>>() {});
            }
        } catch (Exception e) {
            log.warn("读取 AlarmStatistics 缓存失败，回源查询: {}", e.getMessage());
        }

        List<Map<String, Object>> rows = statsMapper.countAlarmByLevel(startTime, endTime);
        List<StatItem> items = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String level = String.valueOf(row.get("level"));
                int cnt = ((Number) row.get("cnt")).intValue();
                items.add(StatItem.builder()
                        .label(translateLevel(level))
                        .value(cnt)
                        .color(colorOfLevel(level))
                        .build());
            }
        }
        try {
            redisUtil.setEx(cacheKey, objectMapper.writeValueAsString(items), cacheTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入 AlarmStatistics 缓存失败: {}", e.getMessage());
        }
        return items;
    }

    @Override
    public List<Map<String, Object>> getHydroStatistics(Long stationId, String startTime, String endTime) {
        String cacheKey = CACHE_KEY_HYDRO_PREFIX + stationId + ":" + startTime + ":" + endTime;
        try {
            String cached = redisUtil.get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("读取 HydroStatistics 缓存失败，回源查询: {}", e.getMessage());
        }

        List<Map<String, Object>> rows = statsMapper.selectHydroStats(stationId, startTime, endTime);
        List<Map<String, Object>> result = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        try {
            redisUtil.setEx(cacheKey, objectMapper.writeValueAsString(result), cacheTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入 HydroStatistics 缓存失败: {}", e.getMessage());
        }
        return result;
    }

    /** 报警级别翻译为中文标签 */
    private String translateLevel(String level) {
        return switch (level) {
            case "INFO" -> "提示";
            case "WARNING" -> "预警";
            case "DANGER" -> "危险";
            case "CRITICAL" -> "紧急";
            default -> level;
        };
    }

    /** 报警级别对应颜色 */
    private String colorOfLevel(String level) {
        return switch (level) {
            case "INFO" -> "#1890ff";
            case "WARNING" -> "#faad14";
            case "DANGER" -> "#ff4d4f";
            case "CRITICAL" -> "#cf1322";
            default -> "#8c8c8c";
        };
    }
}
