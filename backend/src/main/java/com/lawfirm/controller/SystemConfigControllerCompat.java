package com.lawfirm.controller;

import com.lawfirm.service.SystemConfigService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器 - 兼容前端调用路径
 * 前端调用 /api/admin/settings，后端实际是 /api/system-config
 */
@Slf4j
@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
public class SystemConfigControllerCompat {

    private final SystemConfigService systemConfigService;

    /**
     * 获取系统配置
     * GET /api/admin/settings
     */
    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> getSettings(@RequestParam(required = false) String category) {
        try {
            if (category != null && !category.isEmpty()) {
                Map<String, String> result = systemConfigService.getConfigsByCategory(category);
                return Result.success(result);
            }
            // 返回所有分类的配置
            List<Map<String, String>> categories = systemConfigService.getCategories();
            return Result.success(Map.of("categories", categories.toString()));
        } catch (Exception e) {
            log.error("获取系统配置失败", e);
            return Result.error(e.getMessage());
        }
    }
}
