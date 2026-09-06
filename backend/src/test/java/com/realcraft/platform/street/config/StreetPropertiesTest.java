package com.realcraft.platform.street.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreetPropertiesTest {

    @Test
    void defaults() {
        StreetProperties props = new StreetProperties();
        assertEquals(500.0, props.getMaxRadius());
        assertEquals("./data/street-chunks", props.getCacheDir());
        assertEquals(0, props.getMinY());
        assertEquals(255, props.getMaxY());
        assertEquals(16, props.getTileSize());
    }

    @Test
    void settersBindCamelCaseFields() {
        StreetProperties props = new StreetProperties();
        props.setApiKey("test-key");
        props.setApiUrl("https://example.com/tiles");
        props.setMaxRadius(100);
        props.setCacheDir("./custom-cache");
        assertEquals("test-key", props.getApiKey());
        assertEquals("https://example.com/tiles", props.getApiUrl());
        assertEquals(100.0, props.getMaxRadius());
        assertEquals("./custom-cache", props.getCacheDir());
    }
}