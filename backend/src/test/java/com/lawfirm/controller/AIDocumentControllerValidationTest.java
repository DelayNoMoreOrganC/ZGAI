package com.lawfirm.controller;

import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.AIDocumentService;
import com.lawfirm.service.CaseService;
import com.lawfirm.util.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AIDocumentControllerValidationTest {

    private AIDocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new AIDocumentController(
                mock(AIDocumentService.class), mock(CaseService.class), mock(SecurityUtils.class));
    }

    @Test
    void rejectsUnsupportedSingleDocumentBeforeCallingAi() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", new byte[] {1});

        Result<?> result = controller.recognizeLegalDocument(file, null);

        assertEquals(400, result.getCode());
        assertEquals("仅支持图片和PDF文件", result.getMessage());
    }

    @Test
    void batchUsesTheSameFileValidationAsSingleRecognition() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "payload.zip", "application/zip", new byte[] {1});

        Result<?> result = controller.recognizeLegalDocumentsBatch(
                new org.springframework.web.multipart.MultipartFile[] {file}, null);

        assertEquals(400, result.getCode());
        assertEquals("仅支持图片和PDF文件", result.getMessage());
    }
}
