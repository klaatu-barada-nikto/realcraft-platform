package com.realcraft.platform.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTest {

    @Test
    void imagePathDerives() {
        Task t = new Task("abc-123");
        assertEquals("abc-123_0.jpg", t.imagePath(0));
        assertEquals("abc-123_2.jpg", t.imagePath(2));
    }

    @Test
    void modelPathDerives() {
        Task t = new Task("abc-123");
        assertEquals("abc-123.json", t.modelPath());
    }

    @Test
    void randomIdIsUuid() {
        Task t = new Task();
        assertTrue(t.id().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }
}