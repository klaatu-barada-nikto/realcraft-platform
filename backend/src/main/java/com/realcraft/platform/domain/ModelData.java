package com.realcraft.platform.domain;

import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 模型二维数组的领域封装，序列化为顶层紧凑二维数组 [[id,x,y,z],...]。
 */
public record ModelData(List<VoxelBlock> blocks) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 输出紧凑单行 JSON，无包装对象、无额外字段。
     */
    public String serialize() {
        try {
            return MAPPER.writeValueAsString(blocks);
        } catch (Exception e) {
            throw new IllegalStateException("序列化模型失败", e);
        }
    }

    /**
     * 校验模型数据：非空、坐标非负、id 合法。
     */
    public void validate() {
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalStateException("模型数据为空");
        }
        for (VoxelBlock b : blocks) {
            if (b.x() < 0 || b.y() < 0 || b.z() < 0) {
                throw new IllegalStateException("坐标必须为非负整数");
            }
            if (!VoxelBlock.isValidId(b.id())) {
                throw new IllegalStateException("非法方块 ID: " + b.id());
            }
        }
    }
}