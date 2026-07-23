package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.AIPrivacyCleanupPreviewDTO;
import com.lawfirm.dto.AIPrivacyCleanupRequest;
import com.lawfirm.dto.AIPrivacyCleanupResultDTO;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.AIPrivacyCleanupService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("system/ai-privacy")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
public class AIPrivacyController {

    private final AIPrivacyCleanupService cleanupService;
    private final SecurityUtils securityUtils;

    @GetMapping("/preview")
    public Result<AIPrivacyCleanupPreviewDTO> preview() {
        return Result.success(cleanupService.preview());
    }

    @PostMapping("/cleanup")
    @AuditLog(value = "清理历史AI敏感原文", operationType = "PRIVACY_CLEANUP",
            logParams = false, logResult = false)
    public Result<AIPrivacyCleanupResultDTO> cleanup(@Valid @RequestBody AIPrivacyCleanupRequest request) {
        return Result.success(cleanupService.cleanup(request, securityUtils.getCurrentUserId()));
    }
}
