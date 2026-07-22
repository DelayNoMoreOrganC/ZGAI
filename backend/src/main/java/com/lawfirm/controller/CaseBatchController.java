package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.BatchOperationRequest;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.CaseService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 案件批量操作控制器
 */
@Slf4j
@RestController
@RequestMapping("/cases/batch")
@RequiredArgsConstructor
public class CaseBatchController {

    private final CaseService caseService;
    private final SecurityUtils securityUtils;

    /**
     * 批量结案
     * PUT /api/cases/batch/close
     */
    @PutMapping("/close")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "批量结案", operationType = "BATCH_UPDATE", logParams = false)
    public Result<Void> batchCloseCases(@Valid @RequestBody BatchOperationRequest request) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            caseService.batchCloseCases(request.getCaseIds(), currentUserId);
            return Result.success();
        } catch (Exception e) {
            log.error("Batch close cases failed: {}", e.getMessage(), e);
            return Result.error("Batch close failed: " + e.getMessage());
        }
    }

    /**
     * 批量归档
     * PUT /api/cases/batch/archive
     */
    @PutMapping("/archive")
    @PreAuthorize("hasAuthority('CASE_ARCHIVE')")
    @AuditLog(value = "批量归档", operationType = "BATCH_ARCHIVE", logParams = false)
    public Result<Void> batchArchiveCases(@Valid @RequestBody BatchOperationRequest request) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            caseService.batchArchiveCases(request.getCaseIds(), currentUserId);
            return Result.success();
        } catch (Exception e) {
            log.error("Batch archive cases failed: {}", e.getMessage(), e);
            return Result.error("Batch archive failed: " + e.getMessage());
        }
    }

    /**
     * 批量删除
     * DELETE /api/cases/batch
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('CASE_DELETE')")
    @AuditLog(value = "批量删除案件", operationType = "BATCH_DELETE", logParams = false)
    public Result<Void> batchDeleteCases(@Valid @RequestBody BatchOperationRequest request) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            caseService.batchDeleteCases(request.getCaseIds(), currentUserId);
            return Result.success();
        } catch (Exception e) {
            log.error("Batch delete cases failed: {}", e.getMessage(), e);
            return Result.error("Batch delete failed: " + e.getMessage());
        }
    }

    /**
     * 批量修改主办律师
     * PUT /api/cases/batch/change-owner
     */
    @PutMapping("/change-owner")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "批量变更承办人", operationType = "BATCH_UPDATE", logParams = false)
    public Result<Void> batchChangeOwner(@Valid @RequestBody BatchOperationRequest request) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            if (request.getOwnerId() == null) {
                return Result.error("Owner ID cannot be empty");
            }
            caseService.batchChangeOwner(request.getCaseIds(), request.getOwnerId(), currentUserId);
            return Result.success();
        } catch (Exception e) {
            log.error("Batch change owner failed: {}", e.getMessage(), e);
            return Result.error("Batch change owner failed: " + e.getMessage());
        }
    }
}
