package com.realcraft.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源与 SPA 回退装配：{@code /**} 指向固定 dist 目录，由 SpaResourceResolver 兜底。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final String DIST_DIR = "./dist";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + DIST_DIR + "/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }
}