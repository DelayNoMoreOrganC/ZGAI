package com.lawfirm.controller;

import com.lawfirm.dto.AiChatRequest;
import com.lawfirm.service.AiChatService;
import com.lawfirm.util.Result;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI问答控制器
 */
@Slf4j
@RestController
@RequestMapping("ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final SecurityUtils securityUtils;

    /**
     * 普通聊天
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody AiChatRequest request) {
        Long userId = getCurrentUserId();
        String response = aiChatService.generalChat(request.getMessage(), userId);
        return Result.success(response);
    }

    /**
     * AI辅助接口（兼容前端调用）
     * POST /api/ai/assist
     */
    @PostMapping("/assist")
    public Result<String> assist(@RequestBody AiChatRequest request) {
        Long userId = getCurrentUserId();
        String response = aiChatService.generalChat(request.getMessage(), userId);
        return Result.success(response);
    }

    /**
     * 案件上下文问答
     */
    @PostMapping("/case-chat/{caseId}")
    public Result<String> caseChat(@PathVariable Long caseId, @RequestBody AiChatRequest request) {
        Long userId = getCurrentUserId();
        request.setCaseId(caseId);
        String response = aiChatService.caseChat(request, userId);
        return Result.success(response);
    }

    /**
     * 获取当前用户ID（兼容未登录状态）
     */
    private Long getCurrentUserId() {
        try {
            return securityUtils.getCurrentUserId();
        } catch (Exception e) {
            log.warn("获取当前用户失败，使用默认用户(admin): {}", e.getMessage());
            return 1L; // 默认admin用户
        }
    }
}
