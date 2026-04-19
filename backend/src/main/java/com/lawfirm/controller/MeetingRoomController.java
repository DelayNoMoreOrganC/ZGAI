package com.lawfirm.controller;

import com.lawfirm.dto.MeetingRoomDTO;
import com.lawfirm.service.MeetingRoomService;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会议室管理控制器
 */
@Slf4j
@RestController
@RequestMapping("meeting-room")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    /**
     * 创建会议室
     * POST /api/meeting-room
     */
    @PostMapping
    public Result<MeetingRoomDTO> createMeetingRoom(@Valid @RequestBody MeetingRoomDTO dto) {
        try {
            MeetingRoomDTO result = meetingRoomService.createMeetingRoom(dto);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建会议室失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新会议室
     * PUT /api/meeting-room/{id}
     */
    @PutMapping("/{id}")
    public Result<MeetingRoomDTO> updateMeetingRoom(@PathVariable Long id,
                                                   @Valid @RequestBody MeetingRoomDTO dto) {
        try {
            MeetingRoomDTO result = meetingRoomService.updateMeetingRoom(id, dto);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新会议室失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除会议室
     * DELETE /api/meeting-room/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteMeetingRoom(@PathVariable Long id) {
        try {
            meetingRoomService.deleteMeetingRoom(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除会议室失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取会议室列表
     * GET /api/meeting-room
     */
    @GetMapping
    public Result<List<MeetingRoomDTO>> getMeetingRoomList(
            @RequestParam(required = false) Boolean onlyEnabled) {
        try {
            List<MeetingRoomDTO> result = meetingRoomService.getMeetingRoomList(onlyEnabled);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取会议室列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取会议室详情
     * GET /api/meeting-room/{id}
     */
    @GetMapping("/{id}")
    public Result<MeetingRoomDTO> getMeetingRoomDetail(@PathVariable Long id) {
        try {
            MeetingRoomDTO result = meetingRoomService.getMeetingRoomDetail(id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取会议室详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用会议室
     * PUT /api/meeting-room/{id}/status
     */
    @PutMapping("/{id}/status")
    public Result<Void> toggleMeetingRoomStatus(@PathVariable Long id,
                                               @RequestParam Integer status) {
        try {
            meetingRoomService.toggleMeetingRoomStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("切换会议室状态失败", e);
            return Result.error(e.getMessage());
        }
    }
}
