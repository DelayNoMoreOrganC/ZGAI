package com.lawfirm.controller;

import com.lawfirm.service.OcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OcrControllerTest {

    private OcrService ocrService;
    private OcrController controller;

    @BeforeEach
    void setUp() {
        ocrService = mock(OcrService.class);
        controller = new OcrController(ocrService);
    }

    @Test
    void returnsValidationErrorForRejectedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", new byte[]{1});
        when(ocrService.recognizeDocument(file, "document"))
                .thenThrow(new IllegalArgumentException("不支持的文件"));

        var result = controller.recognizeDocument(file, "document");

        assertEquals(400, result.getCode());
        assertEquals("不支持的文件", result.getMessage());
    }

    @Test
    void doesNotExposeInternalOcrFailureDetails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notice.pdf", "application/pdf", new byte[]{1});
        when(ocrService.recognizeDocument(file, "document"))
                .thenThrow(new IllegalStateException("/secret/command/path failed"));

        var result = controller.recognizeDocument(file, "document");

        assertEquals(500, result.getCode());
        assertEquals("OCR识别失败，请检查本地OCR服务状态", result.getMessage());
    }
}
