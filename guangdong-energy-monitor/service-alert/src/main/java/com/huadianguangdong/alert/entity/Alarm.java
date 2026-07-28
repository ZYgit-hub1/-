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
 * 报警实体
 * <p>
 * 对应数据库表 t_alarm，记录由规则引擎触发的报警事件。
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_alarm")
public class Alarm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报警级别：emergency/high/medium/low */
    private String level;

    /** 报警状态：unconfirmed/confirmed/resolved */
    private String status;

    /** 报警内容 */
    private String content;

    /** 电厂 ID */
    private Long plantId;

    /** 电厂名称 */
    private String plantName;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 解除时间 */
    private LocalDateTime resolveTime;

    /** 处理人 */
    private String handler;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标识（0 未删除，1 已删除） */
    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;
}
