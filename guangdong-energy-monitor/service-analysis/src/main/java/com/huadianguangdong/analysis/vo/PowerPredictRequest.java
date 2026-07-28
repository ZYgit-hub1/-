package com.huadianguangdong.analysis.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 发电预测请求 VO
 *
 * <p>发送给 Python 预测服务的气象数据载荷。
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerPredictRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 电厂 ID */
    private Long plantId;

    /** 气象数据序列 */
    private List<WeatherItem> weatherData;

    /** 气象数据项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 温度 */
        private Double temp;
        /** 湿度 */
        private Double humidity;
        /** 风速 */
        private Double windSpeed;
        /** 降雨量 */
        private Double rainfall;
        /** 时间点 */
        private String time;
    }
}
