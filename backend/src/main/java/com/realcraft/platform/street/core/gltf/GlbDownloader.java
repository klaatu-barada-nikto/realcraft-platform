package com.realcraft.platform.street.core.gltf;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.street.config.StreetProperties;
import com.realcraft.platform.street.model.StreetTask;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Google 3D Tiles 瓦片下载：基于 JDK 内置 {@link HttpClient} 下载 GLB 到内存 {@code byte[]}，
 * 通过 {@code X-Goog-Api-Key} 请求头鉴权，密钥不落日志、不返回响应。
 */
@Component
public class GlbDownloader {

    private static final int HTTP_OK = 200;

    private final StreetProperties props;
    private final HttpClient httpClient;

    public GlbDownloader(StreetProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 下载覆盖目标区域的 GLB 瓦片字节流，失败抛 {@link BusinessException}(UPSTREAM_ERROR)。
     */
    public byte[] download(StreetTask task) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getApiUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("X-Goog-Api-Key", props.getApiKey())
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != HTTP_OK) {
                throw new BusinessException(ErrorCode.UPSTREAM_ERROR, "街景瓦片下载失败，HTTP " + response.statusCode());
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new BusinessException(ErrorCode.UPSTREAM_ERROR, "街景瓦片下载结果为空");
            }
            return body;
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.UPSTREAM_ERROR, "街景瓦片下载被中断");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UPSTREAM_ERROR, "街景瓦片下载失败");
        }
    }
}