package com.huadianguangdong.alert.service;

import com.huadianguangdong.common.dto.HydroDataDTO;
import com.huadianguangdong.common.dto.WeatherDataDTO;

/**
 * 规则动作服务接口
 * <p>
 * 由 Drools 规则的 RHS（then 部分）回调，执行报警 / 预警的持久化与推送。
 *
 * @author huadianguangdong
 */
public interface AlertActionService {

    /**
     * 创建报警
     *
     * @param hydroData 水文数据
     * @param reason    报警原因
     */
    void createAlarm(HydroDataDTO hydroData, String reason);

    /**
     * 创建预警
     *
     * @param hydroData 水文数据
     * @param reason    预警原因
     */
    void createWarning(HydroDataDTO hydroData, String reason);

    /**
     * 创建气象预警（预留扩展）
     *
     * @param weatherData 气象数据
     * @param reason      预警原因
     */
    void createWeatherWarning(WeatherDataDTO weatherData, String reason);
}
