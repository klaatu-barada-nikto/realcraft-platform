package com.realcraft.platform.service;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.config.AppProperties;
import com.realcraft.platform.domain.ModelData;
import com.realcraft.platform.domain.Task;
import com.realcraft.platform.domain.VoxelBlock;
import com.realcraft.platform.llm.JsonSanitizer;
import com.realcraft.platform.llm.LlmClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 上传转换主流程编排：校验 → 落盘原图 → AI 推理 → 清洗 → 校验 → 落盘模型 → 返回 json_url。
 */
@Service
public class GenerateService {

    private final AppProperties props;
    private final LlmClient llmClient;
    private final JsonSanitizer sanitizer;
    private final FileStorageService fileStorage;

    public GenerateService(AppProperties props, LlmClient llmClient,
                           JsonSanitizer sanitizer, FileStorageService fileStorage) {
        this.props = props;
        this.llmClient = llmClient;
        this.sanitizer = sanitizer;
        this.fileStorage = fileStorage;
    }

    public String generate(MultipartFile[] images, String host) {
        Task task = new Task();

        if (images == null || (images.length != 1 && images.length != 3)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "输入形态不合法：请上传 1 张拼合三视图或 3 张分立三视图");
        }

        List<byte[]> imageBytes = new ArrayList<>(images.length);
        for (MultipartFile image : images) {
            byte[] data;
            try {
                data = image.getBytes();
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败");
            }
            if (detectImageType(data) == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型不合法：仅支持图片");
            }
            if (image.getSize() > props.maxImageBytes()) {
                throw new BusinessException(ErrorCode.TOO_LARGE);
            }
            imageBytes.add(data);
        }

        for (int i = 0; i < imageBytes.size(); i++) {
            fileStorage.saveImage(task.id(), i, imageBytes.get(i));
        }

        String raw = llmClient.generate(imageBytes.toArray(new byte[0][]));

        List<VoxelBlock> blocks = sanitizer.sanitize(raw);

        ModelData model = new ModelData(blocks);
        try {
            model.validate();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.UNPROCESSABLE);
        }

        String payload = model.serialize();
        fileStorage.saveModel(task.id(), payload);

        return "http://" + host + "/models/" + task.modelPath();
    }

    static String detectImageType(byte[] data) {
        if (data == null || data.length < 4) {
            return null;
        }
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data[0] == (byte) 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
            return "image/png";
        }
        if (data.length >= 6 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F'
                && data[3] == '8' && (data[4] == '7' || data[4] == '9') && data[5] == 'a') {
            return "image/gif";
        }
        if (data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return "image/webp";
        }
        if (data[0] == 'B' && data[1] == 'M') {
            return "image/bmp";
        }
        return null;
    }
}