package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.OcrExtractRequest;
import com.lawfirm.enums.AIFunctionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmExtractServiceTest {

    private AILogService aiLogService;
    private AIGenerationGateway generationGateway;
    private LlmExtractService service;

    @BeforeEach
    void setUp() {
        aiLogService = mock(AILogService.class);
        generationGateway = mock(AIGenerationGateway.class);
        service = new LlmExtractService(aiLogService, generationGateway, new ObjectMapper());
    }

    @Test
    void usesUnifiedGatewayAndLogsOnlyDocumentMetadata() {
        OcrExtractRequest request = new OcrExtractRequest();
        request.setCaseId(12L);
        request.setProviderType("LM_STUDIO");
        request.setDocumentType("判决书");
        request.setOcrText("原告张某，被告李某，身份证号码440101199001011234");
        when(generationGateway.generate(eq("LM_STUDIO"), any(String.class), eq(2048)))
                .thenReturn(new AIGenerationGateway.GenerationResult(
                        "{\"caseNumber\":\"（2026）粤01民初1号\"}", "LM_STUDIO", "qwen-test"));

        Map<String, Object> result = service.extractLegalElements(request, 7L);

        assertEquals("（2026）粤01民初1号", result.get("caseNumber"));
        ArgumentCaptor<String> metadata = ArgumentCaptor.forClass(String.class);
        verify(aiLogService).log(eq(7L), eq(12L), eq(AIFunctionType.OCR_RECOGNITION),
                metadata.capture(), isNull(), any(String.class), isNull(), eq("LM_STUDIO"),
                eq("qwen-test"), eq("SUCCESS"), any(Integer.class), isNull(), isNull());
        assertFalse(metadata.getValue().contains("张某"));
        assertFalse(metadata.getValue().contains("440101"));
    }

    @Test
    void rejectsOversizedTextBeforeCallingModel() {
        OcrExtractRequest request = new OcrExtractRequest();
        request.setOcrText("a".repeat(60001));

        assertThrows(IllegalArgumentException.class,
                () -> service.extractLegalElements(request, 7L));
        verify(generationGateway, never()).generate(any(), any(), any());
    }
}
