package com.huadianguangdong.common.exception;

import com.huadianguangdong.common.api.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 认证异常
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final int code;

    public UnauthorizedException(String message) {
        super(message);
        this.code = ResultCode.UNAUTHORIZED.getCode();
    }

    public UnauthorizedException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public UnauthorizedException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
