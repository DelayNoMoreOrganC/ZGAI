package com.lawfirm.controller;

import com.lawfirm.service.NotificationService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知管理控制器
 */
@Slf4j
@RestController
@RequestMapping("notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    /**
     * 获取通知列表
     * GET /api/notification
     */
    @GetMapping
    public Result<Page<Map<String, Object>>> getNotificationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            Page<Map<String, Object>> result = notificationService.getNotificationList(userId, page, size, category);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取未读通知列表
     * GET /api/notification/unread
     */
    @GetMapping("/unread")
    public Result<List<Map<String, Object>>> getUnreadNotifications() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            List<Map<String, Object>> result = notificationService.getUnreadNotifications(userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取未读通知失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取未读通知数量
     * GET /api/notification/unread-count
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            long count = notificationService.getUnreadCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读数量失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记为已读
     * PUT /api/notification/{id}/read
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            notificationService.markAsRead(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 全部标记为已读
     * PUT /api/notification/read-all
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            notificationService.markAllAsRead(userId);
            return Result.success();
        } catch (Exception e) {
            log.error("全部标记已读失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除通知
     * DELETE /api/notification/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            notificationService.deleteNotification(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除通知失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取通知分类列表
     * GET /api/notification/categories
     */
    @GetMapping("/categories")
    public Result<List<Map<String, String>>> getCategories() {
        try {
            List<Map<String, String>> result = notificationService.getCategories();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取通知分类失败", e);
            return Result.error(e.getMessage());
        }
    }
}
