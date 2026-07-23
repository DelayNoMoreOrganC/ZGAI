package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.*;
import com.lawfirm.entity.ApprovalFlow;
import com.lawfirm.exception.BusinessException;
import com.lawfirm.service.ApprovalService;
import com.lawfirm.service.CaseService;
import com.lawfirm.service.SealApprovalService;
import com.lawfirm.service.SealAttachmentService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.PageResult;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 审批管理控制器
 */
@Slf4j
@RestController
@RequestMapping("approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;
    private final CaseService caseService;
    private final SealApprovalService sealApprovalService;
    private final SealAttachmentService sealAttachmentService;
    private final SecurityUtils securityUtils;

    /**
     * 创建审批
     * POST /api/approval
     */
    @PostMapping
    @PreAuthorize("hasAuthority('APPROVAL_EDIT')")
    @AuditLog(value = "发起审批", operationType = "CREATE", logParams = false)
    public Result<ApprovalDTO> createApproval(@Valid @RequestBody ApprovalCreateRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            if (request.getCaseId() != null) {
                caseService.assertCaseVisible(request.getCaseId(), userId);
            }
            ApprovalDTO result = approvalService.createApproval(request, userId);
            return Result.success(result);
        } catch (AccessDeniedException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建审批失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/seal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('APPROVAL_EDIT')")
    @AuditLog(value = "发起公章用印审批", operationType = "CREATE", logParams = false)
    public Result<ApprovalDTO> createSealApproval(
            @Valid @ModelAttribute SealApprovalRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(sealApprovalService.create(request, file, userId));
    }

    @GetMapping("/{approvalId}/attachments/{attachmentId}/download")
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    public ResponseEntity<Resource> downloadSealAttachment(
            @PathVariable Long approvalId,
            @PathVariable Long attachmentId) throws Exception {
        ApprovalDTO approval = approvalService.getApprovalDetail(approvalId, securityUtils.getCurrentUserId());
        ApprovalAttachmentDTO attachment = approval.getSealAttachments() == null ? null : approval.getSealAttachments().stream()
                .filter(item -> attachmentId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (attachment == null) {
            throw new com.lawfirm.exception.ResourceNotFoundException("用印审批附件", attachmentId);
        }
        Path path = sealAttachmentService.getDownloadPath(approvalId, attachmentId);
        Resource resource = new UrlResource(path.toUri());
        String disposition = ContentDisposition.attachment()
                .filename(attachment.getOriginalFileName(), StandardCharsets.UTF_8)
                .build().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }

    /**
     * 同意审批
     * PUT /api/approval/{id}/approve
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('APPROVAL_EDIT')")
    @AuditLog(value = "同意审批", operationType = "APPROVE", logParams = false)
    public Result<Void> approveApproval(@PathVariable Long id,
                                       @Valid @RequestBody ApprovalDecisionRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            String comments = request.getComments() == null ? "" : request.getComments();
            approvalService.approveApproval(id, comments, userId);
            return Result.success();
        } catch (AccessDeniedException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("同意审批失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 驳回审批
     * PUT /api/approval/{id}/reject
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('APPROVAL_EDIT')")
    @AuditLog(value = "驳回审批", operationType = "REJECT", logParams = false)
    public Result<Void> rejectApproval(@PathVariable Long id,
                                      @Valid @RequestBody ApprovalDecisionRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            String comments = request.getComments() == null ? "" : request.getComments();
            approvalService.rejectApproval(id, comments, userId);
            return Result.success();
        } catch (AccessDeniedException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("驳回审批失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 转审
     * PUT /api/approval/{id}/transfer
     */
    @PutMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('APPROVAL_EDIT')")
    @AuditLog(value = "转交审批", operationType = "TRANSFER", logParams = false)
    public Result<Void> transferApproval(@PathVariable Long id,
                                        @Valid @RequestBody ApprovalTransferRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            Long newApproverId = request.getNewApproverId();
            String comments = request.getComments() == null ? "" : request.getComments();
            approvalService.transferApproval(id, newApproverId, comments, userId);
            return Result.success();
        } catch (AccessDeniedException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("转审失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 撤回审批
     * PUT /api/approval/{id}/withdraw
     */
    @PutMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('APPROVAL_EDIT')")
    @AuditLog(value = "撤回审批", operationType = "WITHDRAW", logParams = false)
    public Result<Void> withdrawApproval(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            approvalService.withdrawApproval(id, userId);
            return Result.success();
        } catch (AccessDeniedException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("撤回审批失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 催办
     * PUT /api/approval/{id}/urge
     */
    @PutMapping("/{id}/urge")
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    @AuditLog(value = "催办审批", operationType = "URGE", logParams = false)
    public Result<Void> urgeApproval(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            approvalService.urgeApproval(id, userId);
            return Result.success();
        } catch (AccessDeniedException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("催办失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取审批列表
     * GET /api/approval
     */
    @GetMapping
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    public Result<PageResult<ApprovalDTO>> getApprovalList(@Valid ApprovalQueryRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            PageResult<ApprovalDTO> result = approvalService.getApprovalList(request, userId);
            return Result.success(result);
        } catch (AccessDeniedException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取审批列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取审批详情
     * GET /api/approval/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    public Result<ApprovalDTO> getApprovalDetail(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            ApprovalDTO result = approvalService.getApprovalDetail(id, userId);
            return Result.success(result);
        } catch (AccessDeniedException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取审批详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取审批流程记录
     * GET /api/approval/{id}/flow
     */
    @GetMapping("/{id}/flow")
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    public Result<List<ApprovalFlow>> getApprovalFlow(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            List<ApprovalFlow> result = approvalService.getApprovalFlow(id, userId);
            return Result.success(result);
        } catch (AccessDeniedException | BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取审批流程失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取审批类型列表
     * GET /api/approval/types
     */
    @GetMapping("/types")
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    public Result<List<Map<String, String>>> getApprovalTypes() {
        try {
            List<Map<String, String>> result = approvalService.getApprovalTypes();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取审批类型失败", e);
            return Result.error(e.getMessage());
        }
    }
}
