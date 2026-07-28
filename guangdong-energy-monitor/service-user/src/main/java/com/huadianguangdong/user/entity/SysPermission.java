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

/**
 * 系统权限实体
 * <p>
 * 权限类型 type 取值：menu 菜单 / button 按钮 / api 接口
 *
 * @author huadianguangdong
 */
@Data
@TableName("t_sys_permission")
public class SysPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限名称 */
    private String name;

    /** 权限编码 */
    private String code;

    /** 类型（menu 菜单 / button 按钮 / api 接口） */
    private String type;

    /** 父权限 ID（0 为根） */
    @TableField("parent_id")
    private Long parentId;

    /** 访问 URL / 前端路由 */
    private String url;

    /** HTTP 请求方法（GET/POST/PUT/DELETE 等） */
    private String method;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除（0 否 1 是） */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
