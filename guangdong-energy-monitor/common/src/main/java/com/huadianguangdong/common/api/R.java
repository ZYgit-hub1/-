package com.huadianguangdong.common.api;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应体
 *
 * @param <T> 业务数据类型
 */
@Data
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /**
     * 失败响应（默认业务错误码）
     */
    public static <T> R<T> fail(String msg) {
        return new R<>(ResultCode.BUSINESS_ERROR.getCode(), msg, null);
    }

    /**
     * 失败响应（自定义错误码）
     */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    /**
     * 失败响应（基于 ResultCode）
     */
    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMsg(), null);
    }
}
