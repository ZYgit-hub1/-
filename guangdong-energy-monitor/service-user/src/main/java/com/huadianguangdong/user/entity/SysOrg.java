package com.huadianguangdong.user.entity;

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
import java.util.List;

/**
 * 系统组织架构实体
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_sys_org")
public class SysOrg implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父组织 ID（0 为根） */
    @TableField("parent_id")
    private Long parentId;

    /** 组织名称 */
    private String name;

    /** 组织编码 */
    private String code;

    /** 排序号 */
    private Integer sort;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除（0 否 1 是） */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 子节点列表（非数据库字段，用于组织树构建） */
    @TableField(exist = false)
    private List<SysOrg> children;
}
