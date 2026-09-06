package com.realcraft.platform.common;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void successSerialization() throws Exception {
        ApiResponse<Map<String, Object>> resp = ApiResponse.ok(Map.of("json_url", "http://x/models/a.json"));
        String json = mapper.writeValueAsString(resp);
        assertTrue(json.contains("\"code\":200"));
        assertTrue(json.contains("\"message\":\"success\""));
        assertTrue(json.contains("\"data\":{\"json_url\":\"http://x/models/a.json\"}"));
    }

    @Test
    void failOmitsData() throws Exception {
        ApiResponse<Void> resp = ApiResponse.fail(ErrorCode.BAD_REQUEST);
        String json = mapper.writeValueAsString(resp);
        assertFalse(json.contains("\"data\""));
        assertTrue(json.contains("\"code\":400"));
    }

    @Test
    void errorCodeMapping() {
        assertEquals(200, ErrorCode.SUCCESS.code());
        assertEquals(400, ErrorCode.BAD_REQUEST.code());
        assertEquals(413, ErrorCode.TOO_LARGE.code());
        assertEquals(422, ErrorCode.UNPROCESSABLE.code());
        assertEquals(502, ErrorCode.UPSTREAM_ERROR.code());
        assertEquals(500, ErrorCode.INTERNAL_ERROR.code());
    }

    @Test
    void businessExceptionFallsBackToDefaultMessage() {
        BusinessException e = new BusinessException(ErrorCode.UPSTREAM_ERROR);
        assertEquals("上游推理失败", e.message());
    }
}