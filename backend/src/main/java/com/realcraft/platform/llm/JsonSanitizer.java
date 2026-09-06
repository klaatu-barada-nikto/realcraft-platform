package com.realcraft.platform.llm;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.domain.VoxelBlock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 清洗管道：噪声剥离 → 截断修复 → 结构校验 → 规范化，任一阶段失败抛 422。
 */
@Component
public class JsonSanitizer {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<VoxelBlock> sanitize(String raw) {
        String candidate = extractCandidate(raw);
        if (candidate == null) {
            throw new BusinessException(ErrorCode.UNPROCESSABLE);
        }

        JsonNode parsed = parse(candidate);
        if (parsed == null) {
            parsed = parse(repairTruncation(candidate));
            if (parsed == null) {
                throw new BusinessException(ErrorCode.UNPROCESSABLE);
            }
        }

        return normalize(parsed);
    }

    private String extractCandidate(String raw) {
        if (raw == null) {
            return null;
        }
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start == -1 || end == -1 || end <= start) {
            return null;
        }
        return raw.substring(start, end + 1);
    }

    private JsonNode parse(String s) {
        try {
            JsonNode node = mapper.readTree(s);
            return node.isArray() ? node : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String repairTruncation(String s) {
        int idx = s.lastIndexOf(']');
        while (idx >= 0) {
            String sub = s.substring(0, idx + 1) + "]";
            try {
                mapper.readTree(sub);
                return sub;
            } catch (Exception e) {
                idx = s.lastIndexOf(']', idx - 1);
            }
        }
        return "";
    }

    private List<VoxelBlock> normalize(JsonNode parsed) {
        if (parsed.isEmpty()) {
            throw new BusinessException(ErrorCode.UNPROCESSABLE);
        }
        List<VoxelBlock> blocks = new ArrayList<>(parsed.size());
        for (JsonNode elem : parsed) {
            if (!elem.isArray() || elem.size() != 4) {
                throw new BusinessException(ErrorCode.UNPROCESSABLE);
            }
            JsonNode idNode = elem.get(0);
            if (!idNode.isTextual() || !VoxelBlock.isValidId(idNode.asText())) {
                throw new BusinessException(ErrorCode.UNPROCESSABLE);
            }
            Integer x = toInt(elem.get(1));
            Integer y = toInt(elem.get(2));
            Integer z = toInt(elem.get(3));
            if (x == null || y == null || z == null) {
                throw new BusinessException(ErrorCode.UNPROCESSABLE);
            }
            if (x < 0 || y < 0 || z < 0) {
                throw new BusinessException(ErrorCode.UNPROCESSABLE);
            }
            blocks.add(new VoxelBlock(idNode.asText(), x, y, z));
        }
        return blocks;
    }

    private Integer toInt(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asInt();
        }
        if (node.isFloatingPointNumber()) {
            double d = node.asDouble();
            if (d != Math.floor(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
                return null;
            }
            return (int) d;
        }
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}