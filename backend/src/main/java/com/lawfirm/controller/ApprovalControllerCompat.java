package com.lawfirm.controller;

import com.lawfirm.dto.ApprovalDTO;
import com.lawfirm.dto.ApprovalQueryRequest;
import com.lawfirm.service.ApprovalService;
import com.lawfirm.util.PageResult;
import com.lawfirm.util.Result;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 审批管理控制器 - 兼容前端调用路径
 * 前端调用 /api/approvals，后端实际是 /api/approval
 */
@Slf4j
@RestController
@RequestMapping("approvals")
@RequiredArgsConstructor
public class ApprovalControllerCompat {

    private final ApprovalService approvalService;
    private final SecurityUtils securityUtils;

    /**
     * 获取待审批列表
     * GET /api/approvals/pending
     */
    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ApprovalDTO>> getPendingApprovals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            ApprovalQueryRequest request = new ApprovalQueryRequest();
            request.setPage(page);
            request.setSize(size);
            request.setStatus("PENDING");
            PageResult<ApprovalDTO> result = approvalService.getApprovalList(request, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取待审批列表失败", e);
            return Result.error(e.getMessage());
        }
    }
}
