package com.huadianguangdong.collector.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 气象数据实体
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_weather_data")
public class WeatherData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 电厂 ID */
    private Long plantId;

    /** 温度（℃） */
    private Double temp;

    /** 湿度（%） */
    private Double humidity;

    /** 风速（m/s） */
    private Double windSpeed;

    /** 风向 */
    private String windDirection;

    /** 降雨量（mm） */
    private Double rainfall;

    /** 记录时间 */
    private LocalDateTime recordTime;
}
