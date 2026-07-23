package com.lawfirm.service;

import com.lawfirm.dto.ApprovalCreateRequest;
import com.lawfirm.dto.ApprovalDTO;
import com.lawfirm.dto.SealApprovalRequest;
import com.lawfirm.entity.User;
import com.lawfirm.exception.InvalidParameterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SealApprovalServiceTest {
    private ApprovalService approvalService;
    private SealAttachmentService attachmentService;
    private UserPermissionService userPermissionService;
    private CaseService caseService;
    private SealApprovalService service;

    @BeforeEach
    void setUp() {
        approvalService = mock(ApprovalService.class);
        attachmentService = mock(SealAttachmentService.class);
        userPermissionService = mock(UserPermissionService.class);
        caseService = mock(CaseService.class);
        service = new SealApprovalService(approvalService, attachmentService, userPermissionService, caseService);
    }

    @Test
    void standaloneRequestRequiresARealFile() {
        SealApprovalRequest request = request();

        InvalidParameterException error = assertThrows(
                InvalidParameterException.class,
                () -> service.create(request, null, 7L));

        assertEquals("参数file无效: 请选择需要用印的文件", error.getMessage());
    }

    @Test
    void uploadedSealRequestRoutesToAdministrativeApprover() {
        SealApprovalRequest request = request();
        User administrative = user(8L);
        ApprovalDTO created = new ApprovalDTO();
        created.setId(21L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "法律意见书.pdf", "application/pdf", "pdf".getBytes());
        when(userPermissionService.findFirstActiveUserByPermission("SEAL_APPROVE", "ADMINISTRATIVE"))
                .thenReturn(Optional.of(administrative));
        when(approvalService.createSealApproval(org.mockito.ArgumentMatchers.any(ApprovalCreateRequest.class),
                org.mockito.ArgumentMatchers.eq(7L))).thenReturn(created);
        when(approvalService.getApprovalDetail(21L, 7L)).thenReturn(created);

        service.create(request, file, 7L);

        ArgumentCaptor<ApprovalCreateRequest> captor = ArgumentCaptor.forClass(ApprovalCreateRequest.class);
        verify(approvalService).createSealApproval(captor.capture(), org.mockito.ArgumentMatchers.eq(7L));
        assertEquals(ApprovalService.TYPE_SEAL, captor.getValue().getApprovalType());
        assertEquals(8L, captor.getValue().getCurrentApproverId());
        verify(attachmentService).attachUpload(21L, file, 7L);
    }

    @Test
    void caseQuickSealReferencesTheExistingCaseDocument() {
        SealApprovalRequest request = request();
        request.setCaseId(30L);
        request.setCaseDocumentId(40L);
        ApprovalDTO created = new ApprovalDTO();
        created.setId(22L);
        when(userPermissionService.findFirstActiveUserByPermission("SEAL_APPROVE", "ADMINISTRATIVE"))
                .thenReturn(Optional.of(user(8L)));
        when(approvalService.createSealApproval(org.mockito.ArgumentMatchers.any(ApprovalCreateRequest.class),
                org.mockito.ArgumentMatchers.eq(7L))).thenReturn(created);
        when(approvalService.getApprovalDetail(22L, 7L)).thenReturn(created);

        service.create(request, null, 7L);

        verify(caseService).assertCaseManageable(30L, 7L);
        verify(attachmentService).attachCaseDocument(22L, 30L, 40L, 7L);
    }

    private SealApprovalRequest request() {
        SealApprovalRequest request = new SealApprovalRequest();
        request.setTitle("法律意见书用印申请");
        request.setContent("提交客户前申请加盖公章");
        return request;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("行政甲");
        user.setRealName("行政甲");
        user.setPosition("行政管理");
        user.setStatus(1);
        user.setDeleted(false);
        return user;
    }
}
