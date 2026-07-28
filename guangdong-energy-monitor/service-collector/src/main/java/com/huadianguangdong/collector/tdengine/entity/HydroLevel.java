package com.huadianguangdong.collector.tdengine.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * TDengine 水文实时数据实体（超级表 hydro_level）
 * <p>
 * 对应 TDengine 超级表 hydro_level，每个水文站一张子表。
 *
 * @author huadianguangdong
 */
@Data
public class HydroLevel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 数据采集时间戳（毫秒精度） */
    private LocalDateTime ts;

    /** 实时水位（m） */
    private Float waterLevel;

    /** 实时流量（m³/s） */
    private Float flow;

    /** 是否超警戒水位（true=超警戒） */
    private Boolean isOverWarning;

    // ===== TAG 列 =====

    /** 水文站 ID（TAG，关联 PostgreSQL t_hydro_station.id） */
    private Long stationId;
}
