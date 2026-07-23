package com.lawfirm.controller;

import com.lawfirm.dto.ApprovalCreateRequest;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.ApprovalService;
import com.lawfirm.service.CaseService;
import com.lawfirm.service.SealApprovalService;
import com.lawfirm.service.SealAttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalControllerAccessTest {

    private ApprovalService approvalService;
    private CaseService caseService;
    private com.lawfirm.service.SealAttachmentService sealAttachmentService;
    private ApprovalController controller;

    @BeforeEach
    void setUp() {
        approvalService = mock(ApprovalService.class);
        caseService = mock(CaseService.class);
        sealAttachmentService = mock(com.lawfirm.service.SealAttachmentService.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        controller = new ApprovalController(
                approvalService,
                caseService,
                mock(SealApprovalService.class),
                sealAttachmentService,
                securityUtils);
    }

    @Test
    void cannotCreateApprovalForAnInvisibleCase() {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setCaseId(99L);
        doThrow(new AccessDeniedException("denied"))
                .when(caseService).assertCaseVisible(99L, 7L);

        assertThrows(AccessDeniedException.class, () -> controller.createApproval(request));
        verify(approvalService, never()).createApproval(request, 7L);
    }

    @Test
    void approvalDetailAccessDenialIsNotConvertedToGenericFailure() {
        when(approvalService.getApprovalDetail(15L, 7L))
                .thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> controller.getApprovalDetail(15L));
    }

    @Test
    void sealAttachmentCannotBeReadBeforeApprovalVisibilityCheck() {
        when(approvalService.getApprovalDetail(15L, 7L))
                .thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> controller.downloadSealAttachment(15L, 20L));
        verify(sealAttachmentService, never()).getDownloadPath(15L, 20L);
    }
}
