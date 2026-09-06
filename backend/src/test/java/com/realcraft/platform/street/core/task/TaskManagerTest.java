package com.realcraft.platform.street.core.task;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.street.config.StreetProperties;
import com.realcraft.platform.street.core.gltf.GlbDownloader;
import com.realcraft.platform.street.core.gltf.JglTfProcessor;
import com.realcraft.platform.street.core.storage.StreetFileStorage;
import com.realcraft.platform.street.model.StreetStartResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(MockitoExtension.class)
class TaskManagerTest {

    @Mock
    private GlbDownloader downloader;

    @Mock
    private JglTfProcessor processor;

    @Mock
    private StreetFileStorage storage;

    private TaskManager manager() {
        return new TaskManager(new StreetProperties(), downloader, processor, storage);
    }

    @Test
    void startRejectsInvalidLat() {
        assertThrows(BusinessException.class, () -> manager().start(91, 0, 10));
        assertThrows(BusinessException.class, () -> manager().start(-91, 0, 10));
    }

    @Test
    void startRejectsInvalidLon() {
        assertThrows(BusinessException.class, () -> manager().start(0, 181, 10));
        assertThrows(BusinessException.class, () -> manager().start(0, -181, 10));
    }

    @Test
    void startRejectsNonPositiveRadius() {
        assertThrows(BusinessException.class, () -> manager().start(0, 0, 0));
        assertThrows(BusinessException.class, () -> manager().start(0, 0, -5));
    }

    @Test
    void startRejectsRadiusOverLimit() {
        StreetProperties props = new StreetProperties();
        props.setMaxRadius(50);
        TaskManager m = new TaskManager(props, downloader, processor, storage);
        BusinessException e = assertThrows(BusinessException.class, () -> m.start(0, 0, 100));
        assertEquals(ErrorCode.BAD_REQUEST, e.errorCode());
    }

    @Test
    void startReturnsTaskIdAndTotalChunks() {

        StreetStartResponse resp = manager().start(0, 0, 8);
        assertNotNull(resp.taskId());
        assertEquals(1, resp.totalChunks());
    }

    @Test
    void isSafeTaskIdRejectsPathTraversal() {
        assertTrue(TaskManager.isSafeTaskId("abc-123"));
        assertFalse(TaskManager.isSafeTaskId("../etc"));
        assertFalse(TaskManager.isSafeTaskId("a/b"));
        assertFalse(TaskManager.isSafeTaskId("a\\b"));
        assertFalse(TaskManager.isSafeTaskId(""));
        assertFalse(TaskManager.isSafeTaskId(null));
    }
}