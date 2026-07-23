package com.lawfirm.controller;

import com.lawfirm.dto.CaseActivityDTO;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.CaseActivityService;
import com.lawfirm.service.CaseService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("cases/{caseId}/activities")
@RequiredArgsConstructor
public class CaseActivityController {
    private final CaseActivityService activityService;
    private final CaseService caseService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CaseActivityDTO>> list(@PathVariable Long caseId) {
        caseService.assertCaseVisible(caseId, securityUtils.getCurrentUserId());
        return Result.success(activityService.list(caseId));
    }
}
