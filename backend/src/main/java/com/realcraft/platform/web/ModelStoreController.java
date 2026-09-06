package com.realcraft.platform.web;

import com.realcraft.platform.service.ModelStoreService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型分发接口：GET /models/{filename}，返回纯 JSON 紧凑二维数组。
 */
@RestController
@RequestMapping("/models")
public class ModelStoreController {

    private final ModelStoreService modelStoreService;

    public ModelStoreController(ModelStoreService modelStoreService) {
        this.modelStoreService = modelStoreService;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getModel(@PathVariable String filename) {
        byte[] data = modelStoreService.readModel(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }
}