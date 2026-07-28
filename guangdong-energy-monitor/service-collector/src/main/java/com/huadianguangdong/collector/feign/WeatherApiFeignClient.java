package com.huadianguangdong.collector.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 外部气象数据源 Feign 客户端
 * <p>
 * 调用第三方气象 API，返回原始天气 JSON。
 *
 * @author huadianguangdong
 */
@FeignClient(name = "weather-api", url = "${external.weather.api-url}")
public interface WeatherApiFeignClient {

    /**
     * 根据城市获取天气数据
     *
     * @param city 城市名称
     * @return 天气原始 JSON 字符串
     */
    @GetMapping
    String getWeather(@RequestParam("city") String city);
}
