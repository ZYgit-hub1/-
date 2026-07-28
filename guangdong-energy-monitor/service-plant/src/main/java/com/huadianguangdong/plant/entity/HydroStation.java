package com.huadianguangdong.plant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huadianguangdong.common.entity.enums.PlantStatus;
import com.huadianguangdong.common.entity.typehandler.GeometryPointTypeHandler;
import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 水文站实体（PostgreSQL + PostGIS）
 * <p>
 * 对应数据库表 t_hydro_station，location 字段为 PostGIS geometry(Point, 4326) 空间类型。
 *
 * @author huadianguangdong
 */
@Data
@TableName(value = "t_hydro_station", autoResultMap = true)
public class HydroStation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 水文站名称（唯一） */
    private String name;

    /** 所属河流/流域：西江/北江/东江/韩江/鉴江/珠江三角洲 */
    private String riverBasin;

    /** WGS84 经纬度空间点（PostGIS geometry(Point,4326)） */
    @TableField(typeHandler = GeometryPointTypeHandler.class)
    private Point location;

    /** 警戒水位（m），超过即触发预警 */
    private BigDecimal warningLevel;

    /** 保证水位（m），超过即防汛应急响应 */
    private BigDecimal guaranteeLevel;

    /** 历史最高水位（m），用于风险评估 */
    private BigDecimal historicalMax;

    /** 测流能力（m³/s），最大可测流量 */
    private BigDecimal flowCapacity;

    /** 所在城市 */
    private String city;

    /** 运行状态：normal/warning/danger/offline */
    private PlantStatus status;

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
