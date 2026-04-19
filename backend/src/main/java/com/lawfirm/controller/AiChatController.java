package com.lawfirm.controller;

import com.lawfirm.dto.AiChatRequest;
import com.lawfirm.service.AiChatService;
import com.lawfirm.util.Result;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI问答控制器
 */
@RestController
@RequestMapping("ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final SecurityUtils securityUtils;

    /**
     * 通用法律问答
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public Result<String> generalChat(@RequestParam String message) {
        Long userId = getCurrentUserId();
        String response = aiChatService.generalChat(message, userId);
        return Result.success(response);
    }

    /**
     * AI辅助接口（兼容前端调用）
     * POST /api/ai/assist
     */
    @PostMapping("/assist")
    @PreAuthorize("isAuthenticated()")
    public Result<String> assist(@RequestBody AiChatRequest request) {
        Long userId = getCurrentUserId();
        String response = aiChatService.generalChat(request.getMessage(), userId);
        return Result.success(response);
    }

    /**
     * 案件上下文问答
     */
    @PostMapping("/case-chat/{caseId}")
    @PreAuthorize("isAuthenticated()")
    public Result<String> caseChat(@PathVariable Long caseId, @RequestBody AiChatRequest request) {
        Long userId = getCurrentUserId();
        request.setCaseId(caseId);
        String response = aiChatService.caseChat(request, userId);
        return Result.success(response);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        return securityUtils.getCurrentUserId();
    }
}
