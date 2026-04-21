package com.lawfirm.controller;

import com.lawfirm.dto.RAGSearchRequest;
import com.lawfirm.service.RAGKnowledgeService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAG知识库控制器
 */
@Slf4j
@RestController
@RequestMapping("/knowledge/rag")
@RequiredArgsConstructor
public class RAGKnowledgeController {

    private final RAGKnowledgeService ragKnowledgeService;
    private final SecurityUtil securityUtil;

    /**
     * RAG检索问答
     * POST /api/knowledge/rag/search
     */
    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> ragSearch(@RequestBody RAGSearchRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            Map<String, Object> result = ragKnowledgeService.ragSearch(
                request.getQuestion(),
                userId
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("RAG检索失败", e);
            return Result.error("RAG检索失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     * GET /api/knowledge/rag/health
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of(
            "status", "ok",
            "service", "RAG Knowledge MVP",
            "version", "1.0.0"
        ));
    }
}
