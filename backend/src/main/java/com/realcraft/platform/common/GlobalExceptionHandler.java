package com.realcraft.platform.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 全局异常处理器：业务异常映射为统一响应（HTTP 恒 200），
 * 模型分发/静态资源的 HTTP 语义（404/403）返回空 body。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        return ApiResponse.fail(e.errorCode().code(), e.message());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> handleStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).build();
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpected(Exception e) {
        return ApiResponse.fail(ErrorCode.INTERNAL_ERROR);
    }
}