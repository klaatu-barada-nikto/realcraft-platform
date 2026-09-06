package com.realcraft.platform.common;

/**
 * 业务错误码六档定义，对齐 Go 版 httpx/errors.go。
 */
public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "输入不合法"),
    TOO_LARGE(413, "文件过大或数量超限"),
    UNPROCESSABLE(422, "数据解析失败"),
    UPSTREAM_ERROR(502, "上游推理失败"),
    INTERNAL_ERROR(500, "存储失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}