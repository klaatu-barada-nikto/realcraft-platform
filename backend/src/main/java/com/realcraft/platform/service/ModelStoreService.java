package com.realcraft.platform.service;

import com.realcraft.platform.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 模型文件读取：文件名安全校验防目录穿越，命中返回字节，未命中 404，非法 403。
 */
@Service
public class ModelStoreService {

    private final AppProperties props;

    public ModelStoreService(AppProperties props) {
        this.props = props;
    }

    public byte[] readModel(String filename) {
        if (!isSafeFilename(filename)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Path path = Path.of(props.getDataDir(), "models", filename);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    static boolean isSafeFilename(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.contains("..")) {
            return false;
        }
        if (name.contains("\\") || name.contains("/")) {
            return false;
        }
        if (Path.of(name).isAbsolute()) {
            return false;
        }
        return Path.of(name).getFileName().toString().equals(name);
    }
}