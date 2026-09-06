package com.realcraft.platform.street.model;

/**
 * 街景转换启动响应：任务标识与总区块数。
 */
public record StreetStartResponse(String taskId, int totalChunks) {
}