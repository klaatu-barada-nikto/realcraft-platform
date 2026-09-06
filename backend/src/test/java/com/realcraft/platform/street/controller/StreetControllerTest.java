package com.realcraft.platform.street.controller;

import com.realcraft.platform.common.ApiResponse;
import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.domain.VoxelBlock;
import com.realcraft.platform.street.core.task.TaskManager;
import com.realcraft.platform.street.model.ReadyChunk;
import com.realcraft.platform.street.model.StreetPullResponse;
import com.realcraft.platform.street.model.StreetStartRequest;
import com.realcraft.platform.street.model.StreetStartResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreetControllerTest {

    private final TaskManager taskManager = mock(TaskManager.class);
    private final StreetController controller = new StreetController(taskManager);

    @Test
    void startReturnsTaskIdAndTotalChunks() {
        when(taskManager.start(0.0, 0.0, 10.0))
                .thenReturn(new StreetStartResponse("task-1", 5));
        ApiResponse<StreetStartResponse> resp = controller.start(new StreetStartRequest(0.0, 0.0, 10.0));
        assertEquals(200, resp.code());
        assertEquals("task-1", resp.data().taskId());
        assertEquals(5, resp.data().totalChunks());
    }

    @Test
    void startPropagatesBusinessException() {
        when(taskManager.start(anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new BusinessException(ErrorCode.BAD_REQUEST, "纬度非法"));
        BusinessException e = assertThrows(BusinessException.class,
                () -> controller.start(new StreetStartRequest(91.0, 0.0, 10.0)));
        assertEquals(ErrorCode.BAD_REQUEST, e.errorCode());
    }

    @Test
    void pullReturnsReadyChunk() {
        List<VoxelBlock> blocks = List.of(new VoxelBlock("minecraft:stone", 1, 2, 3));
        when(taskManager.pull("task-1"))
                .thenReturn(new ReadyChunk("task-1", 3, 4, blocks));
        ApiResponse<StreetPullResponse> resp = controller.pull("task-1");
        assertEquals(200, resp.code());
        assertEquals("task-1", resp.data().taskId());
        assertEquals(3, resp.data().chunkX());
        assertEquals(4, resp.data().chunkZ());
        assertEquals(blocks, resp.data().blocks());
    }

    @Test
    void pullPropagatesNotFound() {
        when(taskManager.pull("unknown"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class, () -> controller.pull("unknown"));
    }

    @Test
    void pullPropagatesAccepted() {
        when(taskManager.pull("pending"))
                .thenThrow(new ResponseStatusException(HttpStatus.ACCEPTED));
        assertThrows(ResponseStatusException.class, () -> controller.pull("pending"));
    }
}
