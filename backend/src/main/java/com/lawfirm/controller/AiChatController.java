package com.lawfirm.controller;

import com.lawfirm.dto.AiChatRequest;
import com.lawfirm.service.AiChatService;
import com.lawfirm.service.CaseService;
import com.lawfirm.util.Result;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI问答控制器
 */
@Slf4j
@RestController
@RequestMapping("ai")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiChatController {

    private final AiChatService aiChatService;
    private final CaseService caseService;
    private final SecurityUtils securityUtils;

    /**
     * 普通聊天
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody AiChatRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        String response = aiChatService.generalChat(request.getMessage(), userId);
        return Result.success(response);
    }

    /**
     * AI辅助接口（兼容前端调用）
     * POST /api/ai/assist
     */
    @PostMapping("/assist")
    public Result<String> assist(@RequestBody AiChatRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        String response = aiChatService.generalChat(request.getMessage(), userId);
        return Result.success(response);
    }

    /**
     * 案件上下文问答
     */
    @PostMapping("/case-chat/{caseId}")
    public Result<String> caseChat(@PathVariable Long caseId, @RequestBody AiChatRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        caseService.assertCaseVisible(caseId, userId);
        request.setCaseId(caseId);
        String response = aiChatService.caseChat(request, userId);
        return Result.success(response);
    }

}
