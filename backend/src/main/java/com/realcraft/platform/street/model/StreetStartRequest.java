package com.realcraft.platform.street.model;

/**
 * 街景转换启动请求：中心经纬度与转换半径（米）。
 */
public record StreetStartRequest(double lat, double lon, double radius) {
}