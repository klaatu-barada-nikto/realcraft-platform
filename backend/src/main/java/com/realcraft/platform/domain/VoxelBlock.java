package com.realcraft.platform.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 方块四元组，序列化为紧凑数组 [id, x, y, z]，满足 Fabric 下游严格解析契约。
 */
public record VoxelBlock(String id, int x, int y, int z) {

    @JsonValue
    public Object[] toJsonArray() {
        return new Object[]{id, x, y, z};
    }

    /**
     * 校验 id 是否符合 Minecraft 命名空间 ID 形态（namespace:path）。
     */
    public static boolean isValidId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        String[] parts = id.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        if (parts[0].isEmpty() || parts[1].isEmpty()) {
            return false;
        }
        return parts[0].chars().allMatch(c -> isValidIdChar((char) c))
                && parts[1].chars().allMatch(c -> isValidIdChar((char) c));
    }

    private static boolean isValidIdChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || c == '_' || c == '.' || c == '-' || c == '/';
    }
}