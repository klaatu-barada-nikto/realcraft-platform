package com.realcraft.platform.street.core.task;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.domain.VoxelBlock;
import com.realcraft.platform.street.config.StreetProperties;
import com.realcraft.platform.street.core.geo.GeoMathUtil;
import com.realcraft.platform.street.core.gltf.GlbDownloader;
import com.realcraft.platform.street.core.gltf.JglTfProcessor;
import com.realcraft.platform.street.core.storage.StreetFileStorage;
import com.realcraft.platform.street.model.ChunkCoord;
import com.realcraft.platform.street.model.ReadyChunk;
import com.realcraft.platform.street.model.StreetStartResponse;
import com.realcraft.platform.street.model.StreetTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 街景任务注册表与调度器：维护任务表，使用 JDK 21 虚拟线程异步执行下载、体素化、落盘与就绪分发。
 */
@Component
public class TaskManager {

    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

    private final Map<String, StreetTask> tasks = new ConcurrentHashMap<>();
    private final StreetProperties props;
    private final GlbDownloader downloader;
    private final JglTfProcessor processor;
    private final StreetFileStorage storage;

    public TaskManager(StreetProperties props, GlbDownloader downloader,
                       JglTfProcessor processor, StreetFileStorage storage) {
        this.props = props;
        this.downloader = downloader;
        this.processor = processor;
        this.storage = storage;
    }

    public StreetStartResponse start(double lat, double lon, double radius) {
        if (lat < -90 || lat > 90) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "纬度非法");
        }
        if (lon < -180 || lon > 180) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "经度非法");
        }
        if (radius <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "半径必须为正数");
        }
        if (radius > props.getMaxRadius()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "半径超出限制");
        }

        String taskId = UUID.randomUUID().toString();
        List<ChunkCoord> grid = GeoMathUtil.computeGrid(lat, lon, radius, props.getTileSize());
        StreetTask task = new StreetTask(taskId, lat, lon, radius, grid.size(), props.getCacheDir());
        tasks.put(taskId, task);
        storage.ensureTaskDir(task);

        Thread.ofVirtual().name("street-" + taskId).start(() -> process(task, grid));

        return new StreetStartResponse(taskId, grid.size());
    }

    private void process(StreetTask task, List<ChunkCoord> grid) {
        for (ChunkCoord coord : grid) {
            try {
                byte[] glb = downloader.download(task);
                List<VoxelBlock> blocks = processor.voxelize(glb, task, coord);
                storage.saveChunk(task, coord.chunkX(), coord.chunkZ(), blocks);
                task.markCompleted();
                task.pushReady(new ReadyChunk(task.taskId(), coord.chunkX(), coord.chunkZ(), blocks));
                log.info("街景区块就绪 taskId={} chunk=({},{}) blocks={}",
                        task.taskId(), coord.chunkX(), coord.chunkZ(), blocks.size());
            } catch (Exception e) {
                task.markFailed();
                log.error("街景区块失败 taskId={} chunk=({},{})",
                        task.taskId(), coord.chunkX(), coord.chunkZ(), e);
            }
        }
        log.info("街景任务结束 taskId={} completed={} failed={}",
                task.taskId(), task.completedChunks(), task.failedChunks());
    }

    public ReadyChunk pull(String taskId) {
        if (!isSafeTaskId(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        StreetTask task = tasks.get(taskId);
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ReadyChunk chunk = task.pollReady();
        if (chunk == null) {
            throw new ResponseStatusException(HttpStatus.ACCEPTED);
        }
        return chunk;
    }

    static boolean isSafeTaskId(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return false;
        }
        if (taskId.contains("..") || taskId.contains("\\") || taskId.contains("/")) {
            return false;
        }
        return true;
    }
}