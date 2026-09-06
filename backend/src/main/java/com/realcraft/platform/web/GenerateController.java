package com.realcraft.platform.web;

import com.realcraft.platform.common.ApiResponse;
import com.realcraft.platform.service.GenerateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传转换接口：POST /api/generate。
 */
@RestController
@RequestMapping("/api")
public class GenerateController {

    private final GenerateService generateService;

    public GenerateController(GenerateService generateService) {
        this.generateService = generateService;
    }

    @PostMapping("/generate")
    public ApiResponse<GenerateData> generate(
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName() + ":" + request.getServerPort();
        }
        String jsonUrl = generateService.generate(images, host);
        return ApiResponse.ok(new GenerateData(jsonUrl));
    }
}