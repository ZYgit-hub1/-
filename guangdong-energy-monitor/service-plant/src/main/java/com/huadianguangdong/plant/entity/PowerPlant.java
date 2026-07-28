package com.huadianguangdong.plant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huadianguangdong.common.entity.enums.PlantStatus;
import com.huadianguangdong.common.entity.enums.PowerPlantType;
import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电厂实体（PostgreSQL + PostGIS）
 * <p>
 * 对应数据库表 t_power_plant，location 字段为 PostGIS geometry(Point, 4326) 空间类型，
 * 在 Java 层使用 JTS Point 表达，通过自定义 TypeHandler 与数据库交互。
 *
 * @author huadianguangdong
 */
@Data
@TableName(value = "t_power_plant", autoResultMap = true)
public class PowerPlant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID（雪花 ID） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 电厂名称（唯一） */
    private String name;

    /** 电厂类型：coal/gas/solar/wind/storage */
    private PowerPlantType type;

    /** WGS84 经纬度空间点（PostGIS geometry(Point,4326)） */
    @TableField(typeHandler = GeometryPointTypeHandler.class)
    private Point location;

    /** 行政区划代码，如 440100（广州市） */
    private String districtCode;

    /** 所属流域：珠江流域/韩江流域/粤东沿海/粤西沿海/粤北内陆 */
    private String riverBasin;

    /** 装机容量（MW） */
    private BigDecimal capacity;

    /** 运行状态：normal/warning/danger/offline */
    private PlantStatus status;

    /** 详细地址 */
    private String address;

    /** 创建时间（带时区） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（带时区） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识（0 未删除，1 已删除） */
    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;
}
