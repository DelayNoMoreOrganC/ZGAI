package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.AIDocumentIntakeConfirmRequest;
import com.lawfirm.dto.AIDocumentIntakeDTO;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.AIDocumentIntakeService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("ai/document-intakes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CASE_VIEW')")
public class AIDocumentIntakeController {
    private final AIDocumentIntakeService service;
    private final SecurityUtils securityUtils;

    @PostMapping
    @AuditLog(value = "AI接收待归案文件", operationType = "AI_DOCUMENT", logParams = false)
    public Result<AIDocumentIntakeDTO> create(@RequestParam("file") MultipartFile file) {
        return Result.success(service.create(file, securityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<AIDocumentIntakeDTO> get(@PathVariable Long id) {
        return Result.success(service.get(id, securityUtils.getCurrentUserId()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "确认AI文件归案", operationType = "AI_CONFIRM", logParams = false)
    public Result<AIDocumentIntakeDTO> confirm(@PathVariable Long id,
                                               @Valid @RequestBody AIDocumentIntakeConfirmRequest request) {
        return Result.success(service.confirm(id, request, securityUtils.getCurrentUserId()));
    }
}
