package com.realcraft.platform.street.core.gltf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JglTfProcessorTest {

    @Test
    void mapColorToBlockIdMatchesNearest() {
        assertEquals("minecraft:stone", JglTfProcessor.mapColorToBlockId(125, 125, 125));
        assertEquals("minecraft:bricks", JglTfProcessor.mapColorToBlockId(150, 97, 83));
        assertEquals("minecraft:snow_block", JglTfProcessor.mapColorToBlockId(240, 248, 248));
    }

    @Test
    void mapColorToBlockIdAlwaysReturnsValidId() {
        String id = JglTfProcessor.mapColorToBlockId(255, 0, 255);
        assertTrue(id.startsWith("minecraft:"));
    }
}