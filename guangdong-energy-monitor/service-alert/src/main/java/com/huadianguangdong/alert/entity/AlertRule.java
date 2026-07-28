package com.huadianguangdong.alert.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.huadianguangdong.common.entity.enums.AlertRuleType;
import com.huadianguangdong.common.entity.enums.RuleLogicType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报警规则实体（PostgreSQL）
 * <p>
 * 对应数据库表 t_alert_rule，存储 Drools 规则引擎的元数据配置。
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_alert_rule")
public class AlertRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称（唯一） */
    private String name;

    /** 业务域规则类型：hydro/weather/fire/equipment/composite */
    private AlertRuleType ruleType;

    /**
     * 规则逻辑类型：static_threshold / dynamic_deviation / combo_logic / trend_warning
     * <p>
     * 决定 Drools 规则的匹配模式，与 ruleType 正交。
     */
    private RuleLogicType logicType;

    /**
     * 规则条件 JSONB
     * <p>
     * 示例：{"metric":"water_level","op":">=","threshold":13.0,"duration":300}
     */
    private String conditionJson;

    /** 优先级 1-100，数字越小优先级越高 */
    private Integer priority;

    /**
     * 作用目标 JSONB
     * <p>
     * 示例：{"plant_ids":[1,2],"station_ids":[3,4],"districts":["440100"]}
     */
    private String targetsJson;

    /** 死区阈值（避免频繁抖动），如水位 ±0.1m 内不重复报警 */
    private BigDecimal deadZone;

    /** 延迟触发秒数，条件持续满足 N 秒后才生成报警 */
    private Integer delaySec;

    /** 是否启用：true 启用 / false 禁用 */
    private Boolean enabled;

    /** 乐观锁版本号，每次更新 +1 */
    @Version
    private Integer version;

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
