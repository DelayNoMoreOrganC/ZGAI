package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.AICaseCommandRequest;
import com.lawfirm.dto.AICaseCommandResponse;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.AICaseCommandService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("ai/case-commands")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CASE_VIEW')")
public class AICaseCommandController {

    private final AICaseCommandService service;
    private final SecurityUtils securityUtils;

    @PostMapping
    @AuditLog(value = "提交案件AI指令", operationType = "AI_COMMAND", logParams = false)
    public Result<AICaseCommandResponse> submit(@Valid @RequestBody AICaseCommandRequest request) {
        return Result.success(service.submit(request, securityUtils.getCurrentUserId()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "确认案件AI操作", operationType = "AI_CONFIRM", logParams = false)
    public Result<AICaseCommandResponse> confirm(@PathVariable Long id) {
        return Result.success(service.confirm(id, securityUtils.getCurrentUserId()));
    }
}
