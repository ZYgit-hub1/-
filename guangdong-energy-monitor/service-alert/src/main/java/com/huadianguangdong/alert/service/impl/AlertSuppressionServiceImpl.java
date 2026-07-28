package com.huadianguangdong.alert.service.impl;

import com.huadianguangdong.alert.entity.AlertRule;
import com.huadianguangdong.alert.mapper.AlertRuleMapper;
import com.huadianguangdong.alert.service.AlertSuppressionService;
import com.huadianguangdong.common.dto.AlertEventDTO;
import com.huadianguangdong.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 报警抑制服务实现
 * <p>
 * 四级抑制链路：延迟 → 死区 → 风暴 → 去重
 * <p>
 * 缓存策略：
 * <ul>
 *   <li>延迟状态、死区状态：内存 ConcurrentHashMap（进程级，重启丢失）</li>
 *   <li>去重状态：Redis（跨实例共享，TTL 24h）</li>
 *   <li>风暴聚合：Redis（跨实例共享，TTL 5min）</li>
 * </ul>
 *
 * @author huadianguangdong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSuppressionServiceImpl implements AlertSuppressionService {

    private final RedisUtil redisUtil;
    private final AlertRuleMapper alertRuleMapper;

    // ===== Redis Key 前缀 =====

    /** 去重 Key 前缀：alert_dedup:{ruleId}:{plantId}，TTL 24h */
    private static final String DEDUP_KEY_PREFIX = "alert_dedup:";

    /** 风暴聚合 Key 前缀：alert_storm:{ruleId}:{districtCode}:{bucket}，TTL 5min */
    private static final String STORM_KEY_PREFIX = "alert_storm:";

    /** 风暴窗口（秒）：5 分钟 */
    private static final long STORM_WINDOW_SEC = 300;

    /** 去重窗口（小时）：24 小时 */
    private static final long DEDUP_WINDOW_HOURS = 24;

    // ===== 内存状态（延迟 + 死区） =====

    /** 延迟状态：key = {ruleId}:{plantId}，value = 首次超限时间 */
    private final Map<String, LocalDateTime> delayState = new ConcurrentHashMap<>();

    /** 死区状态：key = {ruleId}:{plantId}，value = 已触发时的指标值 */
    private final Map<String, Double> deadZoneState = new ConcurrentHashMap<>();

    /** 规则缓存：key = ruleId，value = AlertRule（延迟加载） */
    private final Map<Long, AlertRule> ruleCache = new ConcurrentHashMap<>();

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<AlertEventDTO> filter(List<AlertEventDTO> rawEvents) {
        List<AlertEventDTO> passed = new ArrayList<>();

        for (AlertEventDTO event : rawEvents) {
            try {
                // 1. 延迟检查
                if (!checkDelay(event)) {
                    log.debug("[抑制-延迟] 未达延迟阈值，暂存 ruleId={} plantId={}", event.getRuleId(), event.getPlantId());
                    event.setSuppressed(true);
                    continue;
                }

                // 2. 死区检查（已在报警中且未回落出死区 → 抑制重复报警）
                if (checkDeadZone(event)) {
                    log.debug("[抑制-死区] 仍在死区内，抑制重复 ruleId={} plantId={}", event.getRuleId(), event.getPlantId());
                    event.setSuppressed(true);
                    continue;
                }

                // 3. 风暴抑制（生成聚合 ID）
                String aggregationId = checkStorm(event);
                event.setAggregationId(aggregationId);

                // 4. 去重检查
                if (!checkDedup(event)) {
                    log.info("[抑制-去重] 24h 内已推送过 ruleId={} plantId={}", event.getRuleId(), event.getPlantId());
                    event.setSuppressed(true);
                    // 被去重的事件仍记录到 t_alert_record（suppressed=true），但不推送
                    passed.add(event);
                    continue;
                }

                // 通过全部抑制
                event.setSuppressed(false);
                passed.add(event);

                // 记录死区状态（首次触发时记录指标值）
                if (event.getMetricValue() != null) {
                    deadZoneState.put(stateKey(event.getRuleId(), event.getPlantId()), event.getMetricValue());
                }

            } catch (Exception e) {
                log.error("[抑制] 抑制检查异常 ruleId={} plantId={}", event.getRuleId(), event.getPlantId(), e);
                // 异常时放行（宁报不漏）
                event.setSuppressed(false);
                passed.add(event);
            }
        }

        log.info("[抑制] 原始 {} 条 → 通过 {} 条", rawEvents.size(), passed.size());
        return passed;
    }

    @Override
    public boolean checkDelay(AlertEventDTO event) {
        AlertRule rule = getRule(event.getRuleId());
        if (rule == null || rule.getDelaySec() == null || rule.getDelaySec() <= 0) {
            return true;  // 无延迟要求
        }

        String key = stateKey(event.getRuleId(), event.getPlantId());
        LocalDateTime firstTime = delayState.get(key);

        if (firstTime == null) {
            // 首次超限，记录开始时间
            delayState.put(key, event.getTriggerTime() != null ? event.getTriggerTime() : LocalDateTime.now());
            log.debug("[抑制-延迟] 首次记录 ruleId={} plantId={} delaySec={}",
                    event.getRuleId(), event.getPlantId(), rule.getDelaySec());
            return false;
        }

        // 检查是否达到延迟阈值
        LocalDateTime now = event.getTriggerTime() != null ? event.getTriggerTime() : LocalDateTime.now();
        long elapsedSec = Duration.between(firstTime, now).getSeconds();
        if (elapsedSec >= rule.getDelaySec()) {
            log.debug("[抑制-延迟] 达到延迟阈值 ruleId={} plantId={} elapsed={}s delaySec={}s",
                    event.getRuleId(), event.getPlantId(), elapsedSec, rule.getDelaySec());
            return true;
        }

        return false;
    }

    @Override
    public boolean checkDeadZone(AlertEventDTO event) {
        AlertRule rule = getRule(event.getRuleId());
        if (rule == null || rule.getDeadZone() == null || event.getMetricValue() == null) {
            return false;  // 无死区要求
        }

        String key = stateKey(event.getRuleId(), event.getPlantId());
        Double triggeredValue = deadZoneState.get(key);

        if (triggeredValue == null) {
            // 未在报警中，无需死区检查
            return false;
        }

        // 已在报警中，检查是否回落出死区
        BigDecimal deadZone = rule.getDeadZone();
        double diff = Math.abs(event.getMetricValue() - triggeredValue);

        if (diff > deadZone.doubleValue()) {
            // 回落超出死区 → 消除报警状态
            log.info("[抑制-死区] 指标回落超出死区 ruleId={} plantId={} triggered={} current={} diff={} deadZone={}",
                    event.getRuleId(), event.getPlantId(), triggeredValue, event.getMetricValue(),
                    diff, deadZone);
            deadZoneState.remove(key);
            delayState.remove(key);
            return false;  // 已消除，允许新报警
        }

        // 仍在死区内 → 抑制
        return true;
    }

    @Override
    public String checkStorm(AlertEventDTO event) {
        if (event.getDistrictCode() == null || event.getRuleId() == null) {
            return null;
        }

        // 5 分钟时间桶：对齐到 5 分钟边界
        LocalDateTime now = event.getTriggerTime() != null ? event.getTriggerTime() : LocalDateTime.now();
        long bucket = now.toEpochSecond(java.time.ZoneOffset.UTC) / STORM_WINDOW_SEC;

        String stormKey = STORM_KEY_PREFIX + event.getRuleId() + ":" + event.getDistrictCode() + ":" + bucket;

        // 尝试获取已有聚合 ID
        String existingAggId = redisUtil.get(stormKey);
        if (existingAggId != null) {
            // 同桶已有聚合 ID，复用
            log.debug("[抑制-风暴] 合并到已有聚合 aggId={} ruleId={} district={}", existingAggId, event.getRuleId(), event.getDistrictCode());
            return existingAggId;
        }

        // 首条事件，生成新聚合 ID
        String newAggId = "AGG-" + event.getRuleId() + "-" + event.getDistrictCode() + "-" + bucket;
        redisUtil.setEx(stormKey, newAggId, STORM_WINDOW_SEC, TimeUnit.SECONDS);

        log.info("[抑制-风暴] 新建聚合 aggId={} ruleId={} district={}", newAggId, event.getRuleId(), event.getDistrictCode());
        return newAggId;
    }

    @Override
    public boolean checkDedup(AlertEventDTO event) {
        if (event.getRuleId() == null || event.getPlantId() == null) {
            return true;  // 无法去重，放行
        }

        String dedupKey = DEDUP_KEY_PREFIX + event.getRuleId() + ":" + event.getPlantId();

        // 检查是否已存在去重记录
        Boolean exists = redisUtil.hasKey(dedupKey);
        if (Boolean.TRUE.equals(exists)) {
            return false;  // 24h 内已推送过
        }

        // 首次推送，写入去重标记（TTL 24h）
        redisUtil.setEx(dedupKey, "1", DEDUP_WINDOW_HOURS, TimeUnit.HOURS);
        return true;
    }

    @Override
    public void clearAlertState(Long ruleId, Long plantId) {
        String key = stateKey(ruleId, plantId);
        delayState.remove(key);
        deadZoneState.remove(key);
        redisUtil.del(DEDUP_KEY_PREFIX + ruleId + ":" + plantId);
        log.info("[抑制] 清除报警状态 ruleId={} plantId={}", ruleId, plantId);
    }

    // ===== 私有工具方法 =====

    /**
     * 加载规则（带缓存）
     */
    private AlertRule getRule(Long ruleId) {
        if (ruleId == null) {
            return null;
        }
        return ruleCache.computeIfAbsent(ruleId, id -> alertRuleMapper.selectById(id));
    }

    /**
     * 构造状态 Key
     */
    private String stateKey(Long ruleId, Long plantId) {
        return ruleId + ":" + plantId;
    }
}
