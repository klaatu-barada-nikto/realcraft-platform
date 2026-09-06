package com.realcraft.platform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Bean 装配：注册 AppProperties 并装配带 180s 超时的 RestClient。
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(180_000);
        factory.setReadTimeout(180_000);
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}