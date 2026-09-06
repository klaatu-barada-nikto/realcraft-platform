package com.realcraft.platform.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 启动引导：容器就绪后创建 data/images 与 data/models 目录，失败抛异常终止启动。
 */
@Component
public class DirectoryInitializer implements ApplicationRunner {

    private final AppProperties props;

    public DirectoryInitializer(AppProperties props) {
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Files.createDirectories(Path.of(props.getDataDir(), "images"));
        Files.createDirectories(Path.of(props.getDataDir(), "models"));
    }
}