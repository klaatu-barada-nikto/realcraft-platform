package com.realcraft.platform.service;

import com.realcraft.platform.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelStoreServiceTest {

    @Test
    void isSafeFilename() {
        assertTrue(ModelStoreService.isSafeFilename("abc-123.json"));
        assertTrue(ModelStoreService.isSafeFilename("a.json"));
        assertFalse(ModelStoreService.isSafeFilename(""));
        assertFalse(ModelStoreService.isSafeFilename(null));
        assertFalse(ModelStoreService.isSafeFilename("../secret.json"));
        assertFalse(ModelStoreService.isSafeFilename("..\\secret.json"));
        assertFalse(ModelStoreService.isSafeFilename("a/b.json"));
        assertFalse(ModelStoreService.isSafeFilename("a\\b.json"));
    }

    @Test
    void readsExistingModel(@TempDir Path tempDir) throws IOException {
        AppProperties props = new AppProperties();
        props.setDataDir(tempDir.toString());
        Files.createDirectories(Path.of(tempDir.toString(), "models"));
        Files.writeString(Path.of(tempDir.toString(), "models", "a.json"), "[[\"minecraft:stone\",0,0,0]]");

        ModelStoreService service = new ModelStoreService(props);
        byte[] data = service.readModel("a.json");
        assertEquals("[[\"minecraft:stone\",0,0,0]]", new String(data));
    }

    @Test
    void returns404ForMissing(@TempDir Path tempDir) {
        AppProperties props = new AppProperties();
        props.setDataDir(tempDir.toString());
        ModelStoreService service = new ModelStoreService(props);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.readModel("missing.json"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void returns403ForUnsafeFilename(@TempDir Path tempDir) {
        AppProperties props = new AppProperties();
        props.setDataDir(tempDir.toString());
        ModelStoreService service = new ModelStoreService(props);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.readModel("../secret.json"));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
}