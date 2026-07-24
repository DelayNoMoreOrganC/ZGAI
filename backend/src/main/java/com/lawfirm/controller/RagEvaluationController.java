package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.RagEvaluationCaseDTO;
import com.lawfirm.dto.RagEvaluationCaseRequest;
import com.lawfirm.dto.RagEvaluationRunDTO;
import com.lawfirm.service.RagEvaluationService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge/rag/evaluations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('KNOWLEDGE_MANAGE')")
public class RagEvaluationController {
    private final RagEvaluationService evaluationService;
    private final SecurityUtil securityUtil;

    @GetMapping
    public Result<List<RagEvaluationCaseDTO>> listCases() {
        return Result.success(evaluationService.listCases());
    }

    @GetMapping("/candidates")
    public Result<List<Map<String, Object>>> listCandidateArticles() {
        return Result.success(evaluationService.listCandidateArticles());
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
