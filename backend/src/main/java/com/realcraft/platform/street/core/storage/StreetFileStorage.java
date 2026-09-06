package com.realcraft.platform.street.core.storage;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.domain.ModelData;
import com.realcraft.platform.domain.VoxelBlock;
import com.realcraft.platform.street.model.StreetTask;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 街景区块落盘：复用 {@link ModelData} 输出紧凑二维数组，先建目录再原子写入。
 */
@Component
public class StreetFileStorage {

    public void ensureTaskDir(StreetTask task) {
        try {
            Files.createDirectories(task.cacheDir());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建街景缓存目录失败");
        }
    }

    public void saveChunk(StreetTask task, int chunkX, int chunkZ, List<VoxelBlock> blocks) {
        String payload = new ModelData(blocks).serialize();
        Path path = task.chunkPath(chunkX, chunkZ);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, payload.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "街景区块落盘失败");
        }
    }
}