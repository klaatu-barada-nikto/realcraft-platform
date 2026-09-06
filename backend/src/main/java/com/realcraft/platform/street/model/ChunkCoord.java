package com.realcraft.platform.street.model;

/**
 * 区块网格坐标及其局部坐标系基准点（相对任务中心的 ENU 东/北坐标，单位：米）。
 */
public record ChunkCoord(int chunkX, int chunkZ, double originEast, double originNorth) {
}