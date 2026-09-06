package com.realcraft.platform.service;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.config.AppProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 原图与模型文件的落盘/读取统一封装。
 */
@Service
public class FileStorageService {

    private final AppProperties props;

    public FileStorageService(AppProperties props) {
        this.props = props;
    }

    public void saveImage(String taskId, int index, byte[] data) {
        Path path = Path.of(props.getDataDir(), "images", taskId + "_" + index + ".jpg");
        write(path, data);
    }

    public void saveModel(String taskId, String content) {
        Path path = Path.of(props.getDataDir(), "models", taskId + ".json");
        write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private void write(Path path, byte[] data) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}