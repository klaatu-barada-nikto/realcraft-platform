package com.realcraft.platform.common;

/**
 * 业务异常，携带错误码，作为各层业务失败的统一抛出载体。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String message;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.message());
        this.errorCode = errorCode;
        this.message = message;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String message() {
        return message != null ? message : errorCode.message();
    }
}