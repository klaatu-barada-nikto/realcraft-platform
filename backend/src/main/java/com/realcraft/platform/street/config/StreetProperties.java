package com.realcraft.platform.street.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 街景模块强类型配置绑定：{@code realcraft.google-street.*} 前缀，
 * 环境变量 {@code REALCRAFT_GOOGLE_STREET_*} 通过 relaxed binding 注入。
 */
@ConfigurationProperties(prefix = "realcraft.google-street")
@Validated
public class StreetProperties {

    @NotBlank
    private String apiKey = "";

    @NotBlank
    private String apiUrl = "https://tile.googleapis.com/v1/3dtiles/root.json";

    @Positive
    private double maxRadius = 500.0;

    @NotBlank
    private String cacheDir = "./data/street-chunks";

    @PositiveOrZero
    private int minY = 0;

    @Positive
    private int maxY = 255;

    @Positive
    private int tileSize = 16;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(double maxRadius) {
        this.maxRadius = maxRadius;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public int getTileSize() {
        return tileSize;
    }

    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
    }
}