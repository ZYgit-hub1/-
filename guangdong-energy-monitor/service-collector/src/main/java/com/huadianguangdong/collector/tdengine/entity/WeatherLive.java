package com.huadianguangdong.collector.tdengine.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * TDengine 气象实时数据实体（超级表 weather_live）
 * <p>
 * 对应 TDengine 超级表 weather_live，每个电厂一张子表。
 *
 * @author huadianguangdong
 */
@Data
public class WeatherLive implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 数据采集时间戳（毫秒精度） */
    private LocalDateTime ts;

    /** 温度（℃） */
    private Float temp;

    /** 相对湿度（%） */
    private Float humidity;

    /** 风速（m/s） */
    private Float windSpeed;

    /** 风向角度（0-359°） */
    private Short windDir;

    /** 过去 1 小时累计降雨量（mm） */
    private Float rain1h;

    /** 大气压（hPa） */
    private Float pressure;

    // ===== TAG 列 =====

    /** 电厂 ID（TAG，关联 PostgreSQL t_power_plant.id） */
    private Long plantId;

    /** 行政区划代码（TAG） */
    private String districtCode;
}
