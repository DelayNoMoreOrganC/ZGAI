package com.lawfirm.controller;

import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.AIDocumentService;
import com.lawfirm.service.CaseService;
import com.lawfirm.util.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AIDocumentControllerValidationTest {

    private AIDocumentController controller;
    private CaseService caseService;
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        caseService = mock(CaseService.class);
        securityUtils = mock(SecurityUtils.class);
        controller = new AIDocumentController(mock(AIDocumentService.class), caseService, securityUtils);
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

    @Test
    void caseAccessDenialIsNotConvertedIntoGenericAiFailure() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notice.pdf", "application/pdf", new byte[]{1});
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        doThrow(new AccessDeniedException("denied"))
                .when(caseService).assertCaseVisible(9L, 7L);

        assertThrows(AccessDeniedException.class,
                () -> controller.recognizeLegalDocument(file, 9L));
    }
}
