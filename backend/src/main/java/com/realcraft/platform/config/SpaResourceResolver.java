package com.realcraft.platform.config;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * SPA 回退解析器：dist 下资源不存在时回退 index.html；
 * 对 /api、/models 前缀返回 null（不产生回退，最终 404）。
 */
public class SpaResourceResolver extends PathResourceResolver {

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
        if (resourcePath.contains("..")) {
            return null;
        }
        Resource resource = super.getResource(resourcePath, location);
        if (resource != null && resource.exists() && resource.isReadable()) {
            return resource;
        }
        if (isReservedPrefix(resourcePath)) {
            return null;
        }
        Resource index = location.createRelative("index.html");
        if (index.exists() && index.isReadable()) {
            return index;
        }
        return null;
    }

    private boolean isReservedPrefix(String path) {
        String p = path.startsWith("/") ? path : "/" + path;
        return p.equals("/api") || p.startsWith("/api/")
                || p.equals("/models") || p.startsWith("/models/");
    }
}