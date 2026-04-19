package com.lawfirm.controller;

import com.lawfirm.dto.AttendanceRecordDTO;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.AttendanceRecordService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 考勤管理控制器
 */
@Slf4j
@RestController
@RequestMapping("attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceRecordService attendanceRecordService;
    private final SecurityUtils securityUtils;

    /**
     * 创建考勤申请
     * POST /api/attendance
     */
    @PostMapping
    public Result<AttendanceRecordDTO> create(@Valid @RequestBody AttendanceRecordDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        AttendanceRecordDTO result = attendanceRecordService.create(dto, userId);
        return Result.success("提交申请成功", result);
    }

    /**
     * 更新考勤申请
     * PUT /api/attendance/{id}
     */
    @PutMapping("/{id}")
    // @PreAuthorize("hasAuthority('USER')")
    public Result<AttendanceRecordDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody AttendanceRecordDTO dto) {
        AttendanceRecordDTO result = attendanceRecordService.update(id, dto);
        return Result.success("更新成功", result);
    }

    /**
     * 删除考勤申请
     * DELETE /api/attendance/{id}
     */
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasAuthority('USER')")
    public Result<String> delete(@PathVariable Long id) {
        attendanceRecordService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 查询我的考勤记录
     * GET /api/attendance/my
     */
    @GetMapping("/my")
    // @PreAuthorize("hasAuthority('USER')")
    public Result<List<AttendanceRecordDTO>> getMyRecords() {
        Long userId = securityUtils.getCurrentUserId();
        List<AttendanceRecordDTO> result = attendanceRecordService.getByUserId(userId);
        return Result.success(result);
    }

    /**
     * 查询待审批列表
     * GET /api/attendance/pending
     */
    @GetMapping("/pending")
    // @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public Result<List<AttendanceRecordDTO>> getPendingApprovals() {
        List<AttendanceRecordDTO> result = attendanceRecordService.getPendingApprovals();
        return Result.success(result);
    }

    /**
     * 审批考勤申请
     * PUT /api/attendance/{id}/approve
     */
    @PutMapping("/{id}/approve")
    // @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public Result<AttendanceRecordDTO> approve(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String comment) {
        Long approverId = securityUtils.getCurrentUserId();
        AttendanceRecordDTO result = attendanceRecordService.approve(id, status, comment, approverId);
        return Result.success("审批完成", result);
    }

    /**
     * 获取月度考勤统计
     * GET /api/attendance/stats?year=2026&month=4
     */
    @GetMapping("/stats")
    // @PreAuthorize("hasAuthority('USER')")
    public Result<List<AttendanceRecordDTO>> getMonthlyStats(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "1") int month) {
        Long userId = securityUtils.getCurrentUserId();
        List<AttendanceRecordDTO> result = attendanceRecordService.getMonthlyStats(userId, year, month);
        return Result.success(result);
    }

    /**
     * 查询所有考勤记录（管理员）
     * GET /api/attendance/all
     */
    @GetMapping("/all")
    // @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public Result<List<AttendanceRecordDTO>> getAllRecords() {
        List<AttendanceRecordDTO> result = attendanceRecordService.getByUserId(null);
        return Result.success(result);
    }
}
