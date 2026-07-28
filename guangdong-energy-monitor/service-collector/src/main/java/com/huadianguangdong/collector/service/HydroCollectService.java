package com.huadianguangdong.collector.service;

import com.huadianguangdong.collector.entity.HydroData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 水文数据采集服务
 *
 * @author huadianguangdong
 */
public interface HydroCollectService {

    /**
     * 定时采集水文数据：调用外部水文 API，转换为 HydroDataDTO，
     * 通过 Kafka 发送到 hydro-data-topic，并入库。
     *
     * @return 本次采集入库的水文数据列表
     */
    List<HydroData> collectAndPush();

    /**
     * 查询水文站历史数据
     *
     * @param stationId  水文站 ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 水文数据列表
     */
    List<HydroData> listReadings(Long stationId, LocalDateTime startTime, LocalDateTime endTime);
}
