package com.realcraft.platform.street.model;

import com.realcraft.platform.domain.VoxelBlock;

import java.util.List;

/**
 * 已就绪区块：包含所属任务与区块坐标及该区块的全部方块四元组。
 */
public record ReadyChunk(String taskId, int chunkX, int chunkZ, List<VoxelBlock> blocks) {
}