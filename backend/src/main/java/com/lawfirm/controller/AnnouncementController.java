package com.lawfirm.controller;

import com.lawfirm.dto.AnnouncementCreateRequest;
import com.lawfirm.dto.AnnouncementDTO;
import com.lawfirm.service.AnnouncementService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公告管理控制器
 */
@Slf4j
@RestController
@RequestMapping("announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final SecurityUtils securityUtils;

    /**
     * 创建公告
     * POST /api/announcement
     */
    @PostMapping
    public Result<AnnouncementDTO> createAnnouncement(@Valid @RequestBody AnnouncementCreateRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            AnnouncementDTO result = announcementService.createAnnouncement(request, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建公告失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新公告
     * PUT /api/announcement/{id}
     */
    @PutMapping("/{id}")
    public Result<AnnouncementDTO> updateAnnouncement(@PathVariable Long id,
                                                     @Valid @RequestBody AnnouncementCreateRequest request) {
        try {
            AnnouncementDTO result = announcementService.updateAnnouncement(id, request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新公告失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除公告
     * DELETE /api/announcement/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        try {
            announcementService.deleteAnnouncement(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除公告失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取公告列表
     * GET /api/announcement
     */
    @GetMapping
    public Result<Page<AnnouncementDTO>> getAnnouncementList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            // 如果没有userId，使用默认值1（管理员）
            Long actualUserId = userId != null ? userId : 1L;
            Page<AnnouncementDTO> result = announcementService.getAnnouncementList(page, size, actualUserId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取公告列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取公告详情
     * GET /api/announcement/{id}
     */
    @GetMapping("/{id}")
    public Result<AnnouncementDTO> getAnnouncementDetail(@PathVariable Long id,
                                                         @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            Long actualUserId = userId != null ? userId : 1L;
            AnnouncementDTO result = announcementService.getAnnouncementDetail(id, actualUserId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取公告详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记为已读
     * PUT /api/announcement/{id}/read
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id,
                                   @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            Long actualUserId = userId != null ? userId : 1L;
            announcementService.markAsRead(id, actualUserId);
            return Result.success();
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取未读公告数量
     * GET /api/announcement/unread-count
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            Long actualUserId = userId != null ? userId : 1L;
            long count = announcementService.getUnreadCount(actualUserId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读数量失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取目标范围列表
     * GET /api/announcement/target-scopes
     */
    @GetMapping("/target-scopes")
    public Result<List<Map<String, String>>> getTargetScopes() {
        try {
            List<Map<String, String>> result = announcementService.getTargetScopes();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取目标范围失败", e);
            return Result.error(e.getMessage());
        }
    }
}
