package com.huadianguangdong.collector.service;

import com.huadianguangdong.collector.entity.WeatherData;

import java.util.List;

/**
 * 气象数据采集服务
 *
 * @author huadianguangdong
 */
public interface WeatherCollectService {

    /**
     * 定时采集气象数据：调用外部气象 API，转换为 WeatherDataDTO，
     * 通过 Kafka 发送到 weather-data-topic，并入库。
     *
     * @return 本次采集入库的气象数据列表
     */
    List<WeatherData> collectAndPush();

    /**
     * 查询电厂最新一条气象数据
     *
     * @param plantId 电厂 ID
     * @return 最新气象数据，无则返回 null
     */
    WeatherData getLatestByPlantId(Long plantId);
}
