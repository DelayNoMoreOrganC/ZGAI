package com.lawfirm.service;

import com.lawfirm.dto.ApprovalCreateRequest;
import com.lawfirm.dto.ApprovalDTO;
import com.lawfirm.dto.SealApprovalRequest;
import com.lawfirm.entity.User;
import com.lawfirm.exception.InvalidParameterException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SealApprovalService {
    private final ApprovalService approvalService;
    private final SealAttachmentService sealAttachmentService;
    private final UserPermissionService userPermissionService;
    private final CaseService caseService;

    @Transactional
    public ApprovalDTO create(SealApprovalRequest request, MultipartFile file, Long userId) {
        if (request.getCaseDocumentId() == null && (file == null || file.isEmpty())) {
            throw new InvalidParameterException("file", "请选择需要用印的文件");
        }
        if (request.getCaseDocumentId() != null && file != null && !file.isEmpty()) {
            throw new InvalidParameterException("file", "案件文件用印无需重复上传附件");
        }
        if (request.getCaseDocumentId() != null && request.getCaseId() == null) {
            throw new InvalidParameterException("caseId", "案件文档用印必须关联案件");
        }
        if (request.getCaseId() != null) caseService.assertCaseManageable(request.getCaseId(), userId);
        User approver = userPermissionService.findFirstActiveUserByPermission("SEAL_APPROVE", "ADMINISTRATIVE")
                .orElseThrow(() -> new InvalidParameterException("未找到具有公章用印审批权限的行政账号，请先配置 SEAL_APPROVE 权限"));

        ApprovalCreateRequest approvalRequest = new ApprovalCreateRequest();
        approvalRequest.setApprovalType(ApprovalService.TYPE_SEAL);
        approvalRequest.setTitle(request.getTitle().trim());
        approvalRequest.setContent(request.getContent().trim());
        approvalRequest.setCaseId(request.getCaseId());
        approvalRequest.setCurrentApproverId(approver.getId());
        ApprovalDTO approval = approvalService.createSealApproval(approvalRequest, userId);
        if (request.getCaseDocumentId() != null) {
            sealAttachmentService.attachCaseDocument(approval.getId(), request.getCaseId(), request.getCaseDocumentId(), userId);
        } else {
            sealAttachmentService.attachUpload(approval.getId(), file, userId);
        }
        return approvalService.getApprovalDetail(approval.getId(), userId);
    }
}
