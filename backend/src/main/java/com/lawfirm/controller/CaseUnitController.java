package com.lawfirm.controller;

import com.lawfirm.dto.*;
import com.lawfirm.service.CaseExecutionService;
import com.lawfirm.service.CasePersonnelService;
import com.lawfirm.service.HearingRecordService;
import com.lawfirm.service.PropertyPreservationService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 受理单位控制器（财产保全、执行、庭审）
 */
@Slf4j
@RestController
@RequestMapping("cases/{caseId}/units")
@RequiredArgsConstructor
public class CaseUnitController {

    private final PropertyPreservationService propertyPreservationService;
    private final CaseExecutionService caseExecutionService;
    private final HearingRecordService hearingRecordService;
    private final CasePersonnelService casePersonnelService;
    private final SecurityUtils securityUtils;

    // ==================== 财产保全 ====================

    @GetMapping("/preservations")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<PropertyPreservationDTO>> getPreservations(@PathVariable Long caseId) {
        List<PropertyPreservationDTO> result = propertyPreservationService.getByCaseId(caseId);
        return Result.success(result);
    }

    @PostMapping("/preservations")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<PropertyPreservationDTO> createPreservation(
            @PathVariable Long caseId,
            @Valid @RequestBody PropertyPreservationDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        dto.setCaseId(caseId);
        PropertyPreservationDTO result = propertyPreservationService.create(dto, userId);
        return Result.success("财产保全创建成功", result);
    }

    @PutMapping("/preservations/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<PropertyPreservationDTO> updatePreservation(
            @PathVariable Long caseId,
            @PathVariable Long id,
            @Valid @RequestBody PropertyPreservationDTO dto) {
        PropertyPreservationDTO result = propertyPreservationService.update(id, dto);
        return Result.success("财产保全更新成功", result);
    }

    @DeleteMapping("/preservations/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deletePreservation(@PathVariable Long caseId, @PathVariable Long id) {
        propertyPreservationService.delete(id);
        return Result.success();
    }

    // ==================== 案件执行 ====================

    @GetMapping("/executions")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CaseExecutionDTO>> getExecutions(@PathVariable Long caseId) {
        List<CaseExecutionDTO> result = caseExecutionService.getByCaseId(caseId);
        return Result.success(result);
    }

    @PostMapping("/executions")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CaseExecutionDTO> createExecution(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseExecutionDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        dto.setCaseId(caseId);
        CaseExecutionDTO result = caseExecutionService.create(dto, userId);
        return Result.success("案件执行创建成功", result);
    }

    @PutMapping("/executions/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CaseExecutionDTO> updateExecution(
            @PathVariable Long caseId,
            @PathVariable Long id,
            @Valid @RequestBody CaseExecutionDTO dto) {
        CaseExecutionDTO result = caseExecutionService.update(id, dto);
        return Result.success("案件执行更新成功", result);
    }

    @DeleteMapping("/executions/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deleteExecution(@PathVariable Long caseId, @PathVariable Long id) {
        caseExecutionService.delete(id);
        return Result.success();
    }

    // ==================== 庭审记录 ====================

    @GetMapping("/hearings")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<HearingRecordDTO>> getHearings(@PathVariable Long caseId) {
        List<HearingRecordDTO> result = hearingRecordService.getByCaseId(caseId);
        return Result.success(result);
    }

    @PostMapping("/hearings")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<HearingRecordDTO> createHearing(
            @PathVariable Long caseId,
            @Valid @RequestBody HearingRecordDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        dto.setCaseId(caseId);
        HearingRecordDTO result = hearingRecordService.create(dto, userId);
        return Result.success("庭审记录创建成功", result);
    }

    @PutMapping("/hearings/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<HearingRecordDTO> updateHearing(
            @PathVariable Long caseId,
            @PathVariable Long id,
            @Valid @RequestBody HearingRecordDTO dto) {
        HearingRecordDTO result = hearingRecordService.update(id, dto);
        return Result.success("庭审记录更新成功", result);
    }

    @DeleteMapping("/hearings/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deleteHearing(@PathVariable Long caseId, @PathVariable Long id) {
        hearingRecordService.delete(id);
        return Result.success();
    }

    // ==================== 承办人员 ====================

    @GetMapping("/personnel")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CasePersonnelDTO>> getPersonnel(@PathVariable Long caseId) {
        List<CasePersonnelDTO> result = casePersonnelService.getByCaseId(caseId);
        return Result.success(result);
    }

    @PostMapping("/personnel")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CasePersonnelDTO> createPersonnel(
            @PathVariable Long caseId,
            @Valid @RequestBody CasePersonnelDTO dto) {
        CasePersonnelDTO result = casePersonnelService.create(caseId, dto);
        return Result.success("承办人员创建成功", result);
    }

    @PutMapping("/personnel/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CasePersonnelDTO> updatePersonnel(
            @PathVariable Long caseId,
            @PathVariable Long id,
            @Valid @RequestBody CasePersonnelDTO dto) {
        CasePersonnelDTO result = casePersonnelService.update(caseId, id, dto);
        return Result.success("承办人员更新成功", result);
    }

    @DeleteMapping("/personnel/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deletePersonnel(@PathVariable Long caseId, @PathVariable Long id) {
        casePersonnelService.delete(caseId, id);
        return Result.success();
    }
}
