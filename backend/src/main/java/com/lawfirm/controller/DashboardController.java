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
        try {
            Long userId = securityUtils.getCurrentUserId();
            Map<String, Object> result = dashboardService.getDashboardStats(userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取工作台统计数据失败", e);
            return Result.error("获取工作台统计数据失败");
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
