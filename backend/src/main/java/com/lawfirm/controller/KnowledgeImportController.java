package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.FlkImportPreviewRequest;
import com.lawfirm.dto.KnowledgeImportConfirmRequest;
import com.lawfirm.dto.KnowledgeReviewRequest;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.entity.KnowledgeImportBatch;
import com.lawfirm.entity.KnowledgeImportItem;
import com.lawfirm.service.KnowledgeImportService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class KnowledgeImportController {
    private final KnowledgeImportService importService;
    private final SecurityUtil securityUtil;

    @PostMapping("/knowledge/import-batches/flk/preview")
    @PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
    @AuditLog(value = "建立法规导入预览", operationType = "CREATE", logParams = false)
    public Result<KnowledgeImportBatch> previewFlk(@Valid @RequestBody FlkImportPreviewRequest request) {
        return Result.success(importService.previewFlk(request.getUrls(), securityUtil.getCurrentUserId()));
    }

    @PostMapping("/knowledge/import-batches/flk/starter")
    @PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
    @AuditLog(value = "建立基础法规导入批次", operationType = "CREATE", logParams = false)
    public Result<KnowledgeImportBatch> createStarterFlkBatch() {
        return Result.success(importService.createStarterFlkBatch(securityUtil.getCurrentUserId()));
    }

    @PostMapping("/knowledge/import-batches/{id}/stage")
    @PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
    @AuditLog(value = "暂存法规导入批次", operationType = "IMPORT", logParams = false)
    public Result<KnowledgeImportBatch> stage(@PathVariable Long id) {
        return Result.success(importService.stageFlk(id));
    }

    @PostMapping("/knowledge/import-items/{id}/attachment")
    @PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
    @AuditLog(value = "补传法规官方文件", operationType = "UPLOAD", logParams = false)
    public Result<KnowledgeImportItem> uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return Result.success(importService.attach(id, file));
    }

    @PostMapping("/knowledge/import-batches/firm-policies/scan")
    @PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
    @AuditLog(value = "扫描律所制度目录", operationType = "SCAN", logParams = false)
    public Result<KnowledgeImportBatch> scanPolicies() {
        return Result.success(importService.scanPolicies(securityUtil.getCurrentUserId()));
    }

    @PostMapping("/knowledge/import-batches/{id}/confirm")
    @PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
    @AuditLog(value = "确认知识导入批次", operationType = "IMPORT", logParams = false)
    public Result<KnowledgeImportBatch> confirm(@PathVariable Long id,
                                               @RequestBody(required = false) KnowledgeImportConfirmRequest request) {
        return Result.success(importService.confirm(id, request, securityUtil.getCurrentUserId()));
    }

    @GetMapping("/knowledge/import-batches")
    @PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
    public Result<List<KnowledgeImportBatch>> listBatches() {
        return Result.success(importService.listBatches());
    }

    @GetMapping("/knowledge/import-batches/{id}/items")
    @PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
    public Result<List<KnowledgeImportItem>> listItems(@PathVariable Long id) {
        return Result.success(importService.items(id));
    }

    @PostMapping("/knowledge/articles/{id}/review")
    @PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
    @AuditLog(value = "审核知识条目", operationType = "REVIEW", logParams = false)
    public Result<Map<String, Object>> review(@PathVariable Long id,
                                              @Valid @RequestBody KnowledgeReviewRequest request) {
        KnowledgeArticle article = importService.review(id, request, securityUtil.getCurrentUserId());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", article.getId());
        response.put("title", article.getTitle());
        response.put("reviewStatus", article.getReviewStatus());
        response.put("knowledgeEligible", article.getKnowledgeEligible());
        response.put("indexStatus", article.getIndexStatus());
        return Result.success(response);
    }
}
