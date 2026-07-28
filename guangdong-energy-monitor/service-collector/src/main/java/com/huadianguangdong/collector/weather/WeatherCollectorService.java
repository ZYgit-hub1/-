package com.huadianguangdong.collector.weather;

import com.huadianguangdong.common.dto.WeatherDataDTO;

import java.util.List;

/**
 * 气象数据采集服务接口
 * <p>
 * 职责：定时调用气象 API → 解析清洗 → 写入 TDengine + 推送 Kafka。
 *
 * @author huadianguangdong
 */
public interface WeatherCollectorService {

    /**
     * 定时采集气象数据（每 5 分钟由 Spring Scheduler 触发）
     * <p>
     * 完整链路：
     * <ol>
     *   <li>通过 WebClient 调用气象 API（含 Resilience4j 重试/熔断/超时）</li>
     *   <li>解析 JSON + 厂区代码映射 + 异常值清洗</li>
     *   <li>写入 TDengine weather_live 超级表</li>
     *   <li>推送至 Kafka 主题 weather.raw</li>
     * </ol>
     *
     * @return 本次采集成功入库的数据列表
     */
    List<WeatherDataDTO> collect();

    /**
     * 手动触发采集（供测试 / 手动补偿调用）
     *
     * @param source 指定数据源（null 表示自动选择）
     * @return 采集结果
     */
    List<WeatherDataDTO> collect(DataSourceHealthChecker.ActiveSource source);
}
