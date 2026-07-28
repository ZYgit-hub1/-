package com.huadianguangdong.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志实体
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_audit_log")
public class AuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 操作用户名 */
    private String username;

    /** 业务模块 */
    private String module;

    /** 操作动作 */
    private String action;

    /** 请求参数 */
    private String params;

    /** 请求 IP */
    private String ip;

    /** 操作结果（success 成功 / fail 失败） */
    private String result;

    /** 耗时（ms） */
    @TableField("cost_time")
    private Integer costTime;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
