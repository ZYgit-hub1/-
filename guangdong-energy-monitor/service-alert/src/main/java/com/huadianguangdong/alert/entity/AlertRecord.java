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
 * 报警记录实体（PostgreSQL t_alert_record）
 * <p>
 * 每条 Drools 规则匹配成功且通过抑制检查后，生成一条报警记录。
 * 与 t_alarm 的区别：t_alarm 面向运维流程（确认/解除），t_alert_record 面向规则溯源与审计。
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_alert_record")
public class AlertRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则 ID（关联 t_alert_rule.id） */
    private Long ruleId;

    /** 规则名称（冗余，便于查询） */
    private String ruleName;

    /** 规则逻辑类型：static_threshold / dynamic_deviation / combo_logic / trend_warning */
    private String logicType;

    /** 业务域类型：hydro / weather / fire / equipment / composite */
    private String ruleType;

    /** 电厂 ID */
    private Long plantId;

    /** 水文站 ID */
    private Long stationId;

    /** 行政区划代码 */
    private String districtCode;

    /** 报警级别：emergency / high / medium / low */
    private String level;

    /** 报警内容 */
    private String content;

    /** 触发指标名 */
    private String metric;

    /** 触发时指标值 */
    private Double metricValue;

    /** 阈值 */
    private Double threshold;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    /** 数据源时间戳（采集时间） */
    private LocalDateTime dataTime;

    /** 聚合 ID（风暴抑制用，同区域5分钟内同类报警共享） */
    private String aggregationId;

    /** 是否被抑制（true 表示被合并/去重，不单独推送） */
    private Boolean suppressed;

    /** 推送状态：pending / pushed / failed */
    private String pushStatus;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标识 */
    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;
}
