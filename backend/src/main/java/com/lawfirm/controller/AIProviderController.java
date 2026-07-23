package com.lawfirm.controller;

import com.lawfirm.dto.AIProviderOptionDTO;
import com.lawfirm.service.AIConfigService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("ai/providers")
@RequiredArgsConstructor
public class AIProviderController {

    private final AIConfigService aiConfigService;

    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public Result<List<AIProviderOptionDTO>> getAvailableProviders() {
        return Result.success(aiConfigService.getAvailableProviders());
    }
}
