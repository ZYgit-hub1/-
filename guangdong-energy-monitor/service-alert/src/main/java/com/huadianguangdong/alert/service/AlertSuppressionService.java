package com.huadianguangdong.alert.service;

import com.huadianguangdong.common.dto.AlertEventDTO;

import java.util.List;

/**
 * 报警抑制服务接口
 * <p>
 * 对 Drools 匹配出的报警事件执行四级抑制：
 * <ol>
 *   <li>时间延迟：超限持续 delay_sec 才触发</li>
 *   <li>死区：回落 dead_zone 才消除</li>
 *   <li>风暴抑制：同区域 5 分钟内同类报警合并，生成聚合 ID</li>
 *   <li>重复控制：24h 内相同规则+电厂仅通知 1 次</li>
 * </ol>
 *
 * @author huadianguangdong
 */
public interface AlertSuppressionService {

    /**
     * 对原始报警事件列表执行抑制过滤
     * <p>
     * 通过四级抑制后返回应实际推送的事件（suppressed=false 的为被抑制的合并事件）。
     *
     * @param rawEvents Drools 匹配出的原始事件
     * @return 通过抑制后应推送的事件列表
     */
    List<AlertEventDTO> filter(List<AlertEventDTO> rawEvents);

    /**
     * 检查时间延迟抑制
     * <p>
     * 条件持续满足 delay_sec 才允许触发；未达延迟返回 false。
     *
     * @param event 报警事件
     * @return true 表示通过延迟检查
     */
    boolean checkDelay(AlertEventDTO event);

    /**
     * 检查死区抑制
     * <p>
     * 已触发报警后，指标回落 dead_zone 范围内不消除，超出才消除并清除状态。
     *
     * @param event 报警事件
     * @return true 表示仍在死区内（应抑制）
     */
    boolean checkDeadZone(AlertEventDTO event);

    /**
     * 检查风暴抑制
     * <p>
     * 同区域（districtCode）5 分钟内同类报警合并为聚合事件。
     *
     * @param event 报警事件
     * @return 聚合 ID（首条事件返回新 ID，后续返回相同 ID）
     */
    String checkStorm(AlertEventDTO event);

    /**
     * 检查重复控制
     * <p>
     * 24h 内相同 ruleId + plantId 仅通知 1 次（Redis key: alert_dedup:{ruleId}:{plantId}）。
     *
     * @param event 报警事件
     * @return true 表示首次（允许推送），false 表示 24h 内已推送过
     */
    boolean checkDedup(AlertEventDTO event);

    /**
     * 消除报警状态（指标回归正常后调用，清除延迟/死区/去重缓存）
     *
     * @param ruleId  规则 ID
     * @param plantId 电厂 ID
     */
    void clearAlertState(Long ruleId, Long plantId);
}
