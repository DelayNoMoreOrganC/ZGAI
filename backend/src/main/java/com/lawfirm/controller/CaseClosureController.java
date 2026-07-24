package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.CaseClosureCreateRequest;
import com.lawfirm.dto.CaseClosureRequestDTO;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.CaseClosureService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class CaseClosureController {
    private final CaseClosureService caseClosureService;
    private final SecurityUtils securityUtils;

    @PostMapping("cases/{caseId}/closure-requests")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "发起案件结案申请", operationType = "SUBMIT", logParams = false)
    public Result<CaseClosureRequestDTO> create(@PathVariable Long caseId,
                                                @Valid @RequestBody CaseClosureCreateRequest request) {
        return Result.success("结案申请已提交行政复核",
                caseClosureService.create(caseId, request, securityUtils.getCurrentUserId()));
    }

    @GetMapping("cases/{caseId}/closure-requests/latest")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<CaseClosureRequestDTO> latest(@PathVariable Long caseId) {
        return Result.success(caseClosureService.getLatest(caseId, securityUtils.getCurrentUserId()));
    }
}
