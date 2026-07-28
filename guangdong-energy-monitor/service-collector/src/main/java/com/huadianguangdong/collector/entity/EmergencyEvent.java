package com.huadianguangdong.collector.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应急事件实体
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_emergency_event")
public class EmergencyEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件类型：weather / flood / fire / earthquake / other */
    private String type;

    /** 事件级别 */
    private String level;

    /** 事件标题 */
    private String title;

    /** 事件内容 */
    private String content;

    /** 事发地点 */
    private String location;

    /** 经度 */
    private Double lng;

    /** 纬度 */
    private Double lat;

    /** 发生时间 */
    private LocalDateTime occurTime;

    /** 状态 */
    private String status;
}
