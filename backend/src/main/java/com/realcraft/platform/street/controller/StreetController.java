package com.realcraft.platform.street.controller;

import com.realcraft.platform.common.ApiResponse;
import com.realcraft.platform.street.core.task.TaskManager;
import com.realcraft.platform.street.model.ReadyChunk;
import com.realcraft.platform.street.model.StreetPullResponse;
import com.realcraft.platform.street.model.StreetStartRequest;
import com.realcraft.platform.street.model.StreetStartResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 街景转换接口：POST /api/street/start 启动转换，GET /api/street/pull 流式拉取就绪区块。
 */
@RestController
@RequestMapping("/api/street")
public class StreetController {

    private final TaskManager taskManager;

    public StreetController(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @PostMapping("/start")
    public ApiResponse<StreetStartResponse> start(@RequestBody StreetStartRequest request) {
        return ApiResponse.ok(taskManager.start(request.lat(), request.lon(), request.radius()));
    }

    @GetMapping("/pull")
    public ApiResponse<StreetPullResponse> pull(@RequestParam String taskId) {
        ReadyChunk chunk = taskManager.pull(taskId);
        return ApiResponse.ok(new StreetPullResponse(chunk.taskId(), chunk.chunkX(), chunk.chunkZ(), chunk.blocks()));
    }
}