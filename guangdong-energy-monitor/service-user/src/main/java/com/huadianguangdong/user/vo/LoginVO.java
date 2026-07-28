package com.huadianguangdong.user.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录返回视图对象
 *
 * @author huadianguangdong
 */
@Data
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 访问令牌 */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色编码集合 */
    private List<String> roles;

    /** 权限编码集合 */
    private List<String> permissions;
}
