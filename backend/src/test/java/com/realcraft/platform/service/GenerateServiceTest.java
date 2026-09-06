package com.realcraft.platform.service;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.config.AppProperties;
import com.realcraft.platform.llm.JsonSanitizer;
import com.realcraft.platform.llm.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenerateServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    @TempDir
    Path tempDir;

    private AppProperties props;
    private LlmClient llmClient;
    private FileStorageService fileStorage;

    @BeforeEach
    void setUp() {
        props = new AppProperties();
        props.setDataDir(tempDir.toString());
        props.setMaxImageMb(10);
        llmClient = mock(LlmClient.class);
        fileStorage = mock(FileStorageService.class);
    }

    private GenerateService service() {
        return new GenerateService(props, llmClient, new JsonSanitizer(), fileStorage);
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("images", "a.jpg", "image/jpeg", JPEG);
    }

    @Test
    void generateSuccess() {
        when(llmClient.generate(any(byte[][].class))).thenReturn("[[\"minecraft:stone\",0,0,0]]");
        String jsonUrl = service().generate(new MultipartFile[]{image()}, "example.com");
        assertTrue(jsonUrl.startsWith("http://example.com/models/"));
        assertTrue(jsonUrl.endsWith(".json"));
    }

    @Test
    void rejectsWrongCount() {
        GenerateService service = service();
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.generate(new MultipartFile[]{}, "h"));
        assertEquals(ErrorCode.BAD_REQUEST, e.errorCode());
    }

    @Test
    void rejectsNonImage() {
        MockMultipartFile txt = new MockMultipartFile("images", "a.txt", "text/plain", "hello".getBytes());
        BusinessException e = assertThrows(BusinessException.class,
                () -> service().generate(new MultipartFile[]{txt}, "h"));
        assertEquals(ErrorCode.BAD_REQUEST, e.errorCode());
    }

    @Test
    void rejectsTooLarge() {
        props.setMaxImageMb(1);
        byte[] big = new byte[2 * 1024 * 1024];
        big[0] = (byte) 0xFF;
        big[1] = (byte) 0xD8;
        big[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile("images", "big.jpg", "image/jpeg", big);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service().generate(new MultipartFile[]{file}, "h"));
        assertEquals(ErrorCode.TOO_LARGE, e.errorCode());
    }

    @Test
    void mapsAiFailureToUpstreamError() {
        when(llmClient.generate(any(byte[][].class)))
                .thenThrow(new BusinessException(ErrorCode.UPSTREAM_ERROR));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service().generate(new MultipartFile[]{image()}, "h"));
        assertEquals(ErrorCode.UPSTREAM_ERROR, e.errorCode());
    }

    @Test
    void mapsParseFailureToUnprocessable() {
        when(llmClient.generate(any(byte[][].class))).thenReturn("garbage");
        BusinessException e = assertThrows(BusinessException.class,
                () -> service().generate(new MultipartFile[]{image()}, "h"));
        assertEquals(ErrorCode.UNPROCESSABLE, e.errorCode());
    }

    @Test
    void detectImageTypeRecognizesFormats() {
        assertNotNull(GenerateService.detectImageType(JPEG));
        assertNotNull(GenerateService.detectImageType(
                new byte[]{(byte) 0x89, 'P', 'N', 'G'}));
        assertNotNull(GenerateService.detectImageType(
                new byte[]{'G', 'I', 'F', '8', '9', 'a'}));
        assertNotNull(GenerateService.detectImageType(
                new byte[]{'B', 'M', 0, 0}));
        assertNull(GenerateService.detectImageType("hello".getBytes()));
        assertNull(GenerateService.detectImageType(null));
    }
}