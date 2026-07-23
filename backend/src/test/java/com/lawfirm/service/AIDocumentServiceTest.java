package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.enums.AIFunctionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIDocumentServiceTest {

    private LocalDocumentTextService documentTextService;
    private AIGenerationGateway generationGateway;
    private AILogService aiLogService;
    private AIDocumentService service;

    @BeforeEach
    void setUp() {
        documentTextService = mock(LocalDocumentTextService.class);
        generationGateway = mock(AIGenerationGateway.class);
        aiLogService = mock(AILogService.class);
        service = new AIDocumentService(
                documentTextService, generationGateway, aiLogService, new ObjectMapper());
    }

    @Test
    void recognitionUsesLocalGatewayAndOnlyReturnsSuggestions() throws Exception {
        String extractedText = "广东省某人民法院 民事判决书 原告张某 被告李某";
        when(documentTextService.extract(any(Path.class), eq("judgment.pdf"), eq("application/pdf")))
                .thenReturn(extractedText);
        when(generationGateway.generateLocally(any(String.class), eq(2048)))
                .thenReturn(new AIGenerationGateway.GenerationResult(
                        "{\"caseNumber\":\"（2026）粤01民初1号\",\"documentType\":\"民事判决书\","
                                + "\"plaintiffName\":\"张某\",\"defendantName\":\"李某\"}",
                        "LM_STUDIO", "qwen-test"));

        AIDocumentRecognitionResult result = service.recognizeLegalDocument(
                new MockMultipartFile("file", "judgment.pdf", "application/pdf", new byte[]{1, 2, 3}),
                7L, 9L);

        assertEquals("（2026）粤01民初1号", result.getCaseNumber());
        assertEquals("判决书", result.getDocumentType());
        verify(generationGateway).generateLocally(any(String.class), eq(2048));

        ArgumentCaptor<String> logSummary = ArgumentCaptor.forClass(String.class);
        verify(aiLogService).log(eq(7L), eq(9L), eq(AIFunctionType.OCR_RECOGNITION),
                logSummary.capture(), isNull(), any(String.class), isNull(), eq("LM_STUDIO"),
                eq("qwen-test"), eq("SUCCESS"), any(Integer.class), isNull(), isNull());
        assertFalse(logSummary.getValue().contains("张某"));
        assertTrue(logSummary.getValue().contains("文本长度="));
    }

    @Test
    void temporaryUploadIsDeletedAfterRecognition() throws Exception {
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        when(documentTextService.extract(pathCaptor.capture(), eq("notice.png"), eq("image/png")))
                .thenReturn("开庭通知书内容");
        when(generationGateway.generateLocally(any(String.class), eq(2048)))
                .thenReturn(new AIGenerationGateway.GenerationResult(
                        "{\"documentType\":\"通知书\"}", "LM_STUDIO", "qwen-test"));

        service.recognizeLegalDocument(
                new MockMultipartFile("file", "notice.png", "image/png", new byte[]{3, 2, 1}),
                7L, null);

        assertFalse(Files.exists(pathCaptor.getValue()));
    }
}
