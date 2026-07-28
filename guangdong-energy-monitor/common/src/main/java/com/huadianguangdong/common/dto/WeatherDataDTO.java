package com.huadianguangdong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 气象数据传输对象（含 TDengine 写入所需全部字段）
 *
 * @author huadianguangdong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDataDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 电厂 ID */
    private Long plantId;

    /** 行政区划代码 */
    private String districtCode;

    /** 温度（℃），清洗后可能为 null */
    private Float temp;

    /** 湿度（%） */
    private Float humidity;

    /** 风速（m/s） */
    private Float windSpeed;

    /** 风向角度（0-359°） */
    private Short windDir;

    /** 风向文本（如"东南风"），仅业务展示用 */
    private String windDirection;

    /** 过去 1 小时降雨量（mm） */
    private Float rain1h;

    /** 降雨量（mm），兼容旧字段 */
    private Float rainfall;

    /** 气压（hPa） */
    private Float pressure;

    /** 数据采集时间（ISO 8601） */
    private String ts;
}
