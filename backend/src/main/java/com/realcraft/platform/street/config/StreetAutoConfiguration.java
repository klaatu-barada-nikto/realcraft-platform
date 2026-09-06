package com.realcraft.platform.street.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * 街景模块自动装配：注册 {@link StreetProperties} 并加载 application-street.yml 实现配置隔离。
 *
 * <p>说明：街景模块采用包级子模块方案（{@code com.realcraft.platform.street.*}），
 * 组件已由主应用 {@code @SpringBootApplication} 的组件扫描覆盖（扫描 {@code com.realcraft.platform} 及其子包），
 * 因此此处不再声明 {@code @ComponentScan} 以避免重复注册冲突；本类聚焦于配置绑定与配置源隔离。</p>
 */
@Configuration
@EnableConfigurationProperties(StreetProperties.class)
@PropertySource(value = "classpath:application-street.yml", factory = YamlPropertySourceFactory.class)
public class StreetAutoConfiguration {
}