package com.lawfirm.controller;

import com.lawfirm.dto.MeetingBookingCreateRequest;
import com.lawfirm.dto.MeetingBookingDTO;
import com.lawfirm.service.MeetingBookingService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议室预约控制器
 */
@Slf4j
@RestController
@RequestMapping("meeting-booking")
@RequiredArgsConstructor
public class MeetingBookingController {

    private final MeetingBookingService meetingBookingService;
    private final SecurityUtils securityUtils;

    /**
     * 创建预约
     * POST /api/meeting-booking
     */
    @PostMapping
    public Result<MeetingBookingDTO> createBooking(@Valid @RequestBody MeetingBookingCreateRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            MeetingBookingDTO result = meetingBookingService.createBooking(request, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建预约失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消预约
     * PUT /api/meeting-booking/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelBooking(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            meetingBookingService.cancelBooking(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消预约失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取预约列表
     * GET /api/meeting-booking
     */
    @GetMapping
    public Result<List<MeetingBookingDTO>> getBookingList(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<MeetingBookingDTO> result = meetingBookingService.getBookingList(roomId, startTime, endTime);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取预约列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取我的预约列表
     * GET /api/meeting-booking/my
     */
    @GetMapping("/my")
    public Result<List<MeetingBookingDTO>> getMyBookings() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            List<MeetingBookingDTO> result = meetingBookingService.getMyBookings(userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取我的预约失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取预约详情
     * GET /api/meeting-booking/{id}
     */
    @GetMapping("/{id}")
    public Result<MeetingBookingDTO> getBookingDetail(@PathVariable Long id) {
        try {
            MeetingBookingDTO result = meetingBookingService.getBookingDetail(id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取预约详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查预约冲突
     * GET /api/meeting-booking/check-conflict
     */
    @GetMapping("/check-conflict")
    public Result<Boolean> checkConflict(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            boolean hasConflict = meetingBookingService.checkConflict(roomId, startTime, endTime);
            return Result.success(hasConflict);
        } catch (Exception e) {
            log.error("检查冲突失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 关联开庭日程
     * PUT /api/meeting-booking/{id}/link-hearing
     */
    @PutMapping("/{id}/link-hearing")
    public Result<Void> linkToHearing(@PathVariable Long id,
                                     @RequestParam Long caseId) {
        try {
            meetingBookingService.linkToHearing(id, caseId);
            return Result.success();
        } catch (Exception e) {
            log.error("关联开庭日程失败", e);
            return Result.error(e.getMessage());
        }
    }
}
