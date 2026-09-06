package com.realcraft.platform.domain;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxelBlockTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesAsCompactArray() throws Exception {
        VoxelBlock b = new VoxelBlock("minecraft:stone", 1, 2, 3);
        assertEquals("[\"minecraft:stone\",1,2,3]", mapper.writeValueAsString(b));
    }

    @Test
    void isValidIdAcceptsNamespacePath() {
        assertTrue(VoxelBlock.isValidId("minecraft:stone"));
        assertTrue(VoxelBlock.isValidId("minecraft:red_wool"));
        assertTrue(VoxelBlock.isValidId("a:b"));
    }

    @Test
    void isValidIdRejectsInvalid() {
        assertFalse(VoxelBlock.isValidId(""));
        assertFalse(VoxelBlock.isValidId("stone"));
        assertFalse(VoxelBlock.isValidId(":stone"));
        assertFalse(VoxelBlock.isValidId("minecraft:"));
        assertFalse(VoxelBlock.isValidId("minecraft:St one"));
    }
}