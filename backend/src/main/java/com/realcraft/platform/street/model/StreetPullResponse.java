package com.realcraft.platform.street.model;

import com.realcraft.platform.domain.VoxelBlock;

import java.util.List;

/**
 * 区块拉取响应：任务标识、区块坐标与方块四元组列表。
 */
public record StreetPullResponse(String taskId, int chunkX, int chunkZ, List<VoxelBlock> blocks) {
}