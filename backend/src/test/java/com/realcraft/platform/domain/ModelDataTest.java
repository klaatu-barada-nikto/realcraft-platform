package com.realcraft.platform.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelDataTest {

    @Test
    void serializesAsNestedArray() {
        ModelData m = new ModelData(List.of(
                new VoxelBlock("minecraft:stone", 0, 0, 0),
                new VoxelBlock("minecraft:glass", 1, 0, 1)
        ));
        assertEquals("[[\"minecraft:stone\",0,0,0],[\"minecraft:glass\",1,0,1]]", m.serialize());
    }

    @Test
    void validateRejectsEmpty() {
        ModelData m = new ModelData(List.of());
        assertThrows(IllegalStateException.class, m::validate);
    }

    @Test
    void validateRejectsNegativeCoordinate() {
        ModelData m = new ModelData(List.of(new VoxelBlock("minecraft:stone", -1, 0, 0)));
        assertThrows(IllegalStateException.class, m::validate);
    }

    @Test
    void validateRejectsInvalidId() {
        ModelData m = new ModelData(List.of(new VoxelBlock("stone", 0, 0, 0)));
        assertThrows(IllegalStateException.class, m::validate);
    }

    @Test
    void validateAcceptsValid() {
        ModelData m = new ModelData(List.of(new VoxelBlock("minecraft:stone", 0, 0, 0)));
        m.validate();
    }
}