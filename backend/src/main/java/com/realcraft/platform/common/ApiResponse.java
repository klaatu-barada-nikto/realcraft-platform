package com.realcraft.platform.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一响应结构，JSON 字段为 code/message/data，data 仅在非空时输出。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null);
    }

    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}