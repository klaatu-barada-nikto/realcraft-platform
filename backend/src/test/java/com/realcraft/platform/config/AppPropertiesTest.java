package com.realcraft.platform.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppPropertiesTest {

    @Test
    void defaults() {
        AppProperties props = new AppProperties();
        assertEquals("./data", props.getDataDir());
        assertEquals(10, props.getMaxImageMb());
        assertEquals(8080, props.getServerPort());
    }

    @Test
    void maxImageBytesConverts() {
        AppProperties props = new AppProperties();
        props.setMaxImageMb(3);
        assertEquals(3L * 1024 * 1024, props.maxImageBytes());
    }
}