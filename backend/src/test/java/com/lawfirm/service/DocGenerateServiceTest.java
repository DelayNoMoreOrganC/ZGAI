package com.lawfirm.service;

import com.lawfirm.dto.DocGenerateRequest;
import com.lawfirm.entity.Case;
import com.lawfirm.repository.CaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocGenerateServiceTest {

    @Test
    void refusesToReadCaseBeforeObjectPermissionCheckPasses() {
        AILogService logService = mock(AILogService.class);
        CaseRepository caseRepository = mock(CaseRepository.class);
        CaseService caseService = mock(CaseService.class);
        AIGenerationGateway gateway = mock(AIGenerationGateway.class);
        DocGenerateService service = new DocGenerateService(logService, caseRepository, caseService, gateway);
        DocGenerateRequest request = request();
        doThrow(new AccessDeniedException("无权访问案件"))
                .when(caseService).assertCaseVisible(9L, 3L);

        assertThatThrownBy(() -> service.generateDocument(request, 3L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("无权访问案件");
        verify(caseRepository, never()).findById(any());
        verify(gateway, never()).generate(any(), anyString());
    }

    @Test
    void generatesThroughUnifiedGatewayAfterPermissionCheck() {
        AILogService logService = mock(AILogService.class);
        CaseRepository caseRepository = mock(CaseRepository.class);
        CaseService caseService = mock(CaseService.class);
        AIGenerationGateway gateway = mock(AIGenerationGateway.class);
        DocGenerateService service = new DocGenerateService(logService, caseRepository, caseService, gateway);
        Case caseEntity = new Case();
        caseEntity.setId(9L);
        caseEntity.setCaseName("测试案件");
        when(caseRepository.findById(9L)).thenReturn(Optional.of(caseEntity));
        when(gateway.generate(any(), anyString())).thenReturn(
                new AIGenerationGateway.GenerationResult("文书草稿", "LM_STUDIO", "qwen"));

        assertThat(service.generateDocument(request(), 3L)).isEqualTo("文书草稿");
        verify(caseService).assertCaseVisible(9L, 3L);
        verify(gateway).generate(eq("LM_STUDIO"), org.mockito.ArgumentMatchers.contains("测试案件"));
    }

    private DocGenerateRequest request() {
        DocGenerateRequest request = new DocGenerateRequest();
        request.setCaseId(9L);
        request.setDocumentType("COMPLAINT");
        request.setProviderType("LM_STUDIO");
        return request;
    }
}
