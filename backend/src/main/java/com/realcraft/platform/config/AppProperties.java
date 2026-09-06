package com.realcraft.platform.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 强类型配置绑定：{@code realcraft.*} 前缀，环境变量 {@code REALCRAFT_*} 通过 relaxed binding 注入。
 */
@ConfigurationProperties(prefix = "realcraft")
@Validated
public class AppProperties {

    @NotBlank
    private String aiApiUrl;

    @NotBlank
    private String aiApiKey;

    @NotBlank
    private String aiModel;

    private String dataDir = "./data";

    private int maxImageMb = 10;

    private int serverPort = 8080;

    public String getAiApiUrl() {
        return aiApiUrl;
    }

    public void setAiApiUrl(String aiApiUrl) {
        this.aiApiUrl = aiApiUrl;
    }

    public String getAiApiKey() {
        return aiApiKey;
    }

    public void setAiApiKey(String aiApiKey) {
        this.aiApiKey = aiApiKey;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public int getMaxImageMb() {
        return maxImageMb;
    }

    public void setMaxImageMb(int maxImageMb) {
        this.maxImageMb = maxImageMb;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * 单图片大小上限（字节）：maxImageMb * 1024 * 1024。
     */
    public long maxImageBytes() {
        return (long) maxImageMb * 1024 * 1024;
    }
}