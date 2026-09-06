package com.realcraft.platform.domain;

import java.util.UUID;

/**
 * 任务标识，持有 UUID，派生原图与模型文件路径。
 */
public record Task(String id) {

    public Task() {
        this(UUID.randomUUID().toString());
    }

    public String imagePath(int index) {
        return id + "_" + index + ".jpg";
    }

    public String modelPath() {
        return id + ".json";
    }
}