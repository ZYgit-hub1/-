package com.huadianguangdong.collector.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 水文数据实体
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_hydro_data")
public class HydroData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 水文站 ID */
    private Long stationId;

    /** 水位（m） */
    private Double waterLevel;

    /** 流量（m³/s） */
    private Double flowRate;

    /** 水位趋势：rising / falling / steady */
    private String trend;

    /** 预警级别：normal / watch / warning / flood */
    private String alertLevel;

    /** 采集时间 */
    private LocalDateTime readingTime;
}
