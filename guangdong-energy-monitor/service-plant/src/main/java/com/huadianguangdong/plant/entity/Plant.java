package com.huadianguangdong.plant.entity;

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
 * 电厂实体
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_plant")
public class Plant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 电厂名称 */
    private String name;

    /** 电厂类型：coal/gas/solar/wind/storage */
    private String type;

    /** 经度 */
    private Double lng;

    /** 纬度 */
    private Double lat;

    /** 装机容量（MW） */
    private Integer capacity;

    /** 运行状态：normal/warning/danger/offline */
    private String status;

    /** 详细地址 */
    private String address;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识（0 未删除，1 已删除） */
    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;
}
