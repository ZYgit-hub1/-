package com.huadianguangdong.collector.service;

import com.huadianguangdong.collector.entity.EmergencyEvent;

import java.util.List;

/**
 * 应急事件采集服务
 *
 * @author huadianguangdong
 */
public interface EmergencyCollectService {

    /**
     * 定时采集应急事件并入库
     *
     * @return 本次采集入库的应急事件列表
     */
    List<EmergencyEvent> collectAndPush();
}
