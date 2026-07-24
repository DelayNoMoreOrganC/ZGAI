package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.RagEvaluationCaseDTO;
import com.lawfirm.dto.RagEvaluationCaseRequest;
import com.lawfirm.dto.RagEvaluationImportResult;
import com.lawfirm.dto.RagEvaluationRunDTO;
import com.lawfirm.service.RagEvaluationService;
import com.lawfirm.service.RagEvaluationWorkbookService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge/rag/evaluations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
public class RagEvaluationController {
    private final RagEvaluationService evaluationService;
    private final RagEvaluationWorkbookService workbookService;
    private final SecurityUtil securityUtil;

    @GetMapping
    public Result<List<RagEvaluationCaseDTO>> listCases() {
        return Result.success(evaluationService.listCases());
    }

    @GetMapping("/candidates")
    public Result<List<Map<String, Object>>> listCandidateArticles() {
        return Result.success(evaluationService.listCandidateArticles());
    }

    @GetMapping("/import-template")
    @AuditLog(value = "下载RAG评价样本模板", operationType = "DOWNLOAD", logParams = false)
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] content = workbookService.createTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''RAG%E8%AF%84%E4%BB%B7%E6%A0%B7%E6%9C%AC%E6%A8%A1%E6%9D%BF.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(content.length)
                .body(content);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditLog(value = "导入RAG评价样本", operationType = "IMPORT", logParams = false)
    public Result<RagEvaluationImportResult> importWorkbook(@RequestParam("file") MultipartFile file,
                                                             @RequestParam(defaultValue = "true") boolean dryRun) {
        return Result.success(workbookService.importWorkbook(
                file, dryRun, securityUtil.getCurrentUserId()));
    }

    @PostMapping
    @AuditLog(value = "建立RAG评价样本", operationType = "CREATE", logParams = false)
    public Result<RagEvaluationCaseDTO> createCase(@Valid @RequestBody RagEvaluationCaseRequest request) {
        return Result.success(evaluationService.createCase(request, securityUtil.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    @AuditLog(value = "更新RAG评价样本", operationType = "UPDATE", logParams = false)
    public Result<RagEvaluationCaseDTO> updateCase(@PathVariable Long id,
                                                    @Valid @RequestBody RagEvaluationCaseRequest request) {
        return Result.success(evaluationService.updateCase(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditLog(value = "删除RAG评价样本", operationType = "DELETE", logParams = false)
    public Result<Void> deleteCase(@PathVariable Long id) {
        evaluationService.deleteCase(id);
        return Result.success();
    }

    @PostMapping("/run")
    @AuditLog(value = "运行RAG评价集", operationType = "EVALUATE", logParams = false)
    public Result<Map<String, Object>> runEnabledCases() {
        return Result.success(evaluationService.runEnabledCases(securityUtil.getCurrentUserId()));
    }

    @GetMapping("/runs")
    public Result<List<RagEvaluationRunDTO>> listRuns() {
        return Result.success(evaluationService.listRuns());
    }
}
