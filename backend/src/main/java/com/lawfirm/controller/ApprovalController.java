package com.lawfirm.controller;

import com.lawfirm.dto.*;
import com.lawfirm.entity.ApprovalFlow;
import com.lawfirm.service.ApprovalService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.PageResult;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    private final SecurityUtils securityUtils;

    /**
     * 创建审批
     * POST /api/approval
     */
    @PostMapping
    public Result<ApprovalDTO> createApproval(@Valid @RequestBody ApprovalCreateRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            ApprovalDTO result = approvalService.createApproval(request, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建审批失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 同意审批
     * PUT /api/approval/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public Result<Void> approveApproval(@PathVariable Long id,
                                       @RequestBody Map<String, String> params) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            String comments = params.getOrDefault("comments", "");
            approvalService.approveApproval(id, comments, userId);
            return Result.success();
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
    public Result<Void> rejectApproval(@PathVariable Long id,
                                      @RequestBody Map<String, String> params) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            String comments = params.getOrDefault("comments", "");
            approvalService.rejectApproval(id, comments, userId);
            return Result.success();
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
    public Result<Void> transferApproval(@PathVariable Long id,
                                        @RequestBody Map<String, Object> params) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            Long newApproverId = Long.valueOf(params.get("newApproverId").toString());
            String comments = params.getOrDefault("comments", "").toString();
            approvalService.transferApproval(id, newApproverId, comments, userId);
            return Result.success();
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
    public Result<Void> withdrawApproval(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            approvalService.withdrawApproval(id, userId);
            return Result.success();
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
    public Result<Void> urgeApproval(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            approvalService.urgeApproval(id, userId);
            return Result.success();
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
    public Result<PageResult<ApprovalDTO>> getApprovalList(ApprovalQueryRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            PageResult<ApprovalDTO> result = approvalService.getApprovalList(request, userId);
            return Result.success(result);
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
    public Result<ApprovalDTO> getApprovalDetail(@PathVariable Long id) {
        try {
            ApprovalDTO result = approvalService.getApprovalDetail(id);
            return Result.success(result);
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
    public Result<List<ApprovalFlow>> getApprovalFlow(@PathVariable Long id) {
        try {
            List<ApprovalFlow> result = approvalService.getApprovalFlow(id);
            return Result.success(result);
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
