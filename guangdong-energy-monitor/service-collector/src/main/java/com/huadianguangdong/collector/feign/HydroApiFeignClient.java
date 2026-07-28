package com.huadianguangdong.collector.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 外部水文数据源 Feign 客户端
 * <p>
 * 调用第三方水文 API，返回水位原始 JSON。
 *
 * @author huadianguangdong
 */
@FeignClient(name = "hydro-api", url = "${external.hydro.api-url}")
public interface HydroApiFeignClient {

    /**
     * 根据水文站编码获取水位数据
     *
     * @param stationCode 水文站编码
     * @return 水位原始 JSON 字符串
     */
    @GetMapping
    String getWaterLevel(@RequestParam("stationCode") String stationCode);
}
