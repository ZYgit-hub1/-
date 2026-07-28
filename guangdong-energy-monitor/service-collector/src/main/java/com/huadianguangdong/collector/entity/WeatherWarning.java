package com.huadianguangdong.collector.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huadianguangdong.common.entity.enums.WarningLevel;
import com.huadianguangdong.common.entity.enums.WeatherWarningType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 气象预警实体（PostgreSQL，按月分区表）
 * <p>
 * 对应数据库表 t_weather_warning，存储 CMA/省气象局发布的预警信号。
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_weather_warning")
public class WeatherWarning implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 受影响行政区划代码 */
    private String districtCode;

    /** 预警类型 */
    private WeatherWarningType type;

    /** 预警级别 */
    private WarningLevel level;

    /** 预警发布时间（分区键） */
    private LocalDateTime publishTime;

    /** 预警失效时间，NULL 表示长期有效 */
    private LocalDateTime expireTime;

    /**
     * 预警详情 JSONB
     * <p>
     * 示例：{"title":"广州市暴雨橙色预警","description":"...","areas":["天河区","越秀区"],"defense":["..."]}
     */
    private String contentJson;

    /** 预警来源：CMA/省气象局/市气象局 */
    private String source;

    /** 状态：active生效中 / expired已过期 / cancelled已撤销 */
    private String status;

    /** 创建时间（带时区） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标识（0 未删除，1 已删除） */
    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;
}
