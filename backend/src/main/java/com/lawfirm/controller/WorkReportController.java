package com.lawfirm.controller;

import com.lawfirm.dto.WorkReportDTO;
import com.lawfirm.service.WorkReportService;
import com.lawfirm.util.Result;
import com.lawfirm.vo.WorkReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 工作汇报Controller
 */
@RestController
@RequestMapping("/work-reports")
@RequiredArgsConstructor
@Tag(name = "工作汇报管理", description = "工作汇报的增删改查接口")
public class WorkReportController {

    private final WorkReportService workReportService;

    @PostMapping
    @Operation(summary = "创建汇报")
    public Result<WorkReportVO> createReport(@Valid @RequestBody WorkReportDTO dto) {
        WorkReportVO vo = workReportService.createReport(dto);
        return Result.success("汇报创建成功", vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新汇报")
    public Result<WorkReportVO> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody WorkReportDTO dto) {
        WorkReportVO vo = workReportService.updateReport(id, dto);
        return Result.success("汇报更新成功", vo);
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "提交汇报")
    public Result<Void> submitReport(@PathVariable Long id) {
        workReportService.submitReport(id);
        return Result.success();
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "审核汇报")
    @PreAuthorize("hasAuthority('WORK_REPORT_REVIEW')")
    public Result<Void> reviewReport(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String comment) {
        workReportService.reviewReport(id, status, comment);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除汇报")
    public Result<Void> deleteReport(@PathVariable Long id) {
        workReportService.deleteReport(id);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "获取汇报列表")
    public Result<Page<WorkReportVO>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<WorkReportVO> result = workReportService.listReports(page, size, status);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取汇报详情")
    public Result<WorkReportVO> getReport(@PathVariable Long id) {
        WorkReportVO vo = workReportService.getReport(id);
        return Result.success(vo);
    }

    @GetMapping("/my")
    @Operation(summary = "我的汇报列表")
    public Result<Page<WorkReportVO>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportDate"));
        Page<WorkReportVO> result = workReportService.getMyReports(page, size);
        return Result.success(result);
    }

    @GetMapping("/pending")
    @Operation(summary = "待审核汇报列表")
    @PreAuthorize("hasAuthority('WORK_REPORT_REVIEW')")
    public Result<Page<WorkReportVO>> getPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportDate"));
        Page<WorkReportVO> result = workReportService.getPendingReports(page, size);
        return Result.success(result);
    }

    @GetMapping("/department/{department}")
    @Operation(summary = "部门汇报列表")
    public Result<List<WorkReportVO>> getDepartmentReports(@PathVariable String department) {
        List<WorkReportVO> result = workReportService.getDepartmentReports(department);
        return Result.success(result);
    }
}
