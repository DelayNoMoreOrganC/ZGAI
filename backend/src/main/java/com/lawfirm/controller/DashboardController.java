package com.lawfirm.controller;

import com.lawfirm.service.DashboardService;
import com.lawfirm.util.Result;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 工作台控制器
 */
@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;

    /**
     * 获取工作台统计数据
     * GET /api/dashboard/stats
     * 从当前登录用户获取userId
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getDashboardStats() {
        log.info("========== 开始获取工作台统计数据 ==========");
        try {
            log.info("Step 1: 准备调用 securityUtils.getCurrentUserId()");
            Long userId = securityUtils.getCurrentUserId();
            log.info("Step 1 成功: userId = {}", userId);

            log.info("Step 2: 准备调用 dashboardService.getDashboardStats(userId={})", userId);
            Map<String, Object> result = dashboardService.getDashboardStats(userId);
            log.info("Step 2 成功: result = {}", result);

            log.info("========== 成功获取工作台统计数据 ==========");
            return Result.success(result);
        } catch (Exception e) {
            log.error("========== 获取工作台统计数据失败 ==========");
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常信息: {}", e.getMessage());
            log.error("完整堆栈:", e);
            return Result.error("获取工作台统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户工作台详情
     * GET /api/dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getUserDashboard() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            Map<String, Object> result = dashboardService.getUserDashboard(userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户工作台详情异常", e);
            return Result.error("获取用户工作台详情失败");
        }
    }
}
