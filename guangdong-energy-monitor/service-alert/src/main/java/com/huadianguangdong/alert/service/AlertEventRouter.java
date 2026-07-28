package com.huadianguangdong.alert.service;

import com.huadianguangdong.common.dto.AlertEventDTO;

import java.util.List;

/**
 * 报警事件路由服务接口
 * <p>
 * 将通过抑制后的 AlertEvent 持久化到 t_alert_record，并路由到推送服务。
 *
 * @author huadianguangdong
 */
public interface AlertEventRouter {

    /**
     * 路由报警事件
     * <p>
     * 1. 转换为 AlertRecord 持久化到 t_alert_record
     * 2. 推送到 Kafka alert.event 主题（供推送服务消费）
     * 3. 未被抑制的事件立即触发多通道推送
     *
     * @param events 报警事件列表
     */
    void route(List<AlertEventDTO> events);

    /**
     * 路由单条报警事件
     *
     * @param event 报警事件
     */
    void route(AlertEventDTO event);
}
