package com.huadianguangdong.alert.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预警实体
 * <p>
 * 对应数据库表 t_warning，记录由规则引擎触发的预警事件（等级低于报警）。
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_warning")
public class Warning implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警级别：green/blue/yellow/orange/red */
    private String level;

    /** 预警类型：weather/flood/fire/equipment/other */
    private String type;

    /** 预警内容 */
    private String content;

    /** 电厂 ID */
    private Long plantId;

    /** 电厂名称 */
    private String plantName;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 状态：active/expired/cancelled */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标识（0 未删除，1 已删除） */
    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;
}
