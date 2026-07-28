package com.huadianguangdong.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),

    /** 参数错误 */
    PARAM_ERROR(400, "参数错误"),

    /** 未认证 */
    UNAUTHORIZED(401, "未认证或认证失效"),

    /** Token 过期 */
    TOKEN_EXPIRED(402, "Token 已过期"),

    /** 无权限 */
    FORBIDDEN(403, "无访问权限"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 业务错误 */
    BUSINESS_ERROR(500, "业务处理失败"),

    /** 系统错误 */
    SYSTEM_ERROR(5000, "系统异常");

    /** 状态码 */
    private final int code;

    /** 提示信息 */
    private final String msg;
}
