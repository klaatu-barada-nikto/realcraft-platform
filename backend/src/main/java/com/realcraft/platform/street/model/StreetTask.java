package com.realcraft.platform.street.model;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 街景任务聚合根：持有任务元数据、区块缓存目录、完成/失败计数与就绪区块队列。
 */
public class StreetTask {

    private final String taskId;
    private final double centerLat;
    private final double centerLon;
    private final double radius;
    private final int totalChunks;
    private final Path cacheDir;

    private final AtomicInteger completedChunks = new AtomicInteger();
    private final AtomicInteger failedChunks = new AtomicInteger();
    private final ConcurrentLinkedQueue<ReadyChunk> readyQueue = new ConcurrentLinkedQueue<>();

    public StreetTask(String taskId, double centerLat, double centerLon, double radius,
                      int totalChunks, String cacheDir) {
        this.taskId = taskId;
        this.centerLat = centerLat;
        this.centerLon = centerLon;
        this.radius = radius;
        this.totalChunks = totalChunks;
        this.cacheDir = Path.of(cacheDir, taskId);
    }

    public String taskId() {
        return taskId;
    }

    public double centerLat() {
        return centerLat;
    }

    public double centerLon() {
        return centerLon;
    }

    public double radius() {
        return radius;
    }

    public int totalChunks() {
        return totalChunks;
    }

    public Path cacheDir() {
        return cacheDir;
    }

    public int completedChunks() {
        return completedChunks.get();
    }

    public int failedChunks() {
        return failedChunks.get();
    }

    /**
     * 派生区块文件路径：{@code cache-dir/{taskId}/chunk_{x}_{z}.json}。
     */
    public Path chunkPath(int chunkX, int chunkZ) {
        return cacheDir.resolve("chunk_" + chunkX + "_" + chunkZ + ".json");
    }

    /**
     * 任务是否进入终态（完成 + 失败 == 总区块数）。
     */
    public boolean isFinished() {
        return completedChunks.get() + failedChunks.get() >= totalChunks;
    }

    public void markCompleted() {
        completedChunks.incrementAndGet();
    }

    public void markFailed() {
        failedChunks.incrementAndGet();
    }

    public void pushReady(ReadyChunk chunk) {
        readyQueue.add(chunk);
    }

    public ReadyChunk pollReady() {
        return readyQueue.poll();
    }
}