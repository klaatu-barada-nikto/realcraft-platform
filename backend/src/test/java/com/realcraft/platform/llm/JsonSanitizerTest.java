package com.realcraft.platform.llm;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.domain.VoxelBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonSanitizerTest {

    private final JsonSanitizer sanitizer = new JsonSanitizer();

    @Test
    void stripsMarkdownFenceAndProse() {
        List<VoxelBlock> blocks = sanitizer.sanitize(
                "好的，以下是模型：\n```json\n[[\"minecraft:stone\",0,0,0]]\n```\n希望有帮助");
        assertEquals(1, blocks.size());
        assertEquals(new VoxelBlock("minecraft:stone", 0, 0, 0), blocks.get(0));
    }

    @Test
    void repairsTruncatedTail() {
        List<VoxelBlock> blocks = sanitizer.sanitize(
                "[[\"minecraft:stone\",0,0,0],[\"minecraft:glass\",1,1,1],[\"minecraft:wool\",2,2,2");
        assertEquals(2, blocks.size());
        assertEquals(new VoxelBlock("minecraft:glass", 1, 1, 1), blocks.get(1));
    }

    @Test
    void acceptsStringNumericCoords() {
        List<VoxelBlock> blocks = sanitizer.sanitize("[[\"minecraft:stone\",\"0\",\"1\",\"2\"]]");
        assertEquals(1, blocks.size());
        assertEquals(new VoxelBlock("minecraft:stone", 0, 1, 2), blocks.get(0));
    }

    @Test
    void rejectsNoBracket() {
        assertThrows(BusinessException.class, () -> sanitizer.sanitize("no json here"));
    }

    @Test
    void rejectsEmptyArray() {
        BusinessException e = assertThrows(BusinessException.class, () -> sanitizer.sanitize("[]"));
        assertEquals(ErrorCode.UNPROCESSABLE, e.errorCode());
    }

    @Test
    void rejectsNegativeCoordinate() {
        assertThrows(BusinessException.class,
                () -> sanitizer.sanitize("[[\"minecraft:stone\",-1,0,0]]"));
    }

    @Test
    void rejectsInvalidId() {
        assertThrows(BusinessException.class,
                () -> sanitizer.sanitize("[[\"Stone\",0,0,0]]"));
    }

    @Test
    void rejectsNonQuadruple() {
        assertThrows(BusinessException.class,
                () -> sanitizer.sanitize("[[\"minecraft:stone\",0,0]]"));
    }
}