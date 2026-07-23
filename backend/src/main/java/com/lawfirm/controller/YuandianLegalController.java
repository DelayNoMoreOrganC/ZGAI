package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.KnowledgeArticleVO;
import com.lawfirm.dto.YuandianCitationCheckRequest;
import com.lawfirm.dto.YuandianImportRequest;
import com.lawfirm.dto.YuandianSearchRequest;
import com.lawfirm.dto.YuandianSearchResponse;
import com.lawfirm.service.YuandianLegalService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/legal-sources/yuandian")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class YuandianLegalController {

    private final YuandianLegalService yuandianLegalService;

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.success(yuandianLegalService.getStatus());
    }

    @PostMapping("/laws/search")
    @AuditLog(value = "元典法规检索", operationType = "SEARCH", logParams = false, logResult = false)
    public Result<YuandianSearchResponse> searchLaws(@Valid @RequestBody YuandianSearchRequest request) {
        return Result.success(yuandianLegalService.searchLaws(request));
    }

    @PostMapping("/cases/search")
    @AuditLog(value = "元典案例检索", operationType = "SEARCH", logParams = false, logResult = false)
    public Result<YuandianSearchResponse> searchCases(@Valid @RequestBody YuandianSearchRequest request) {
        return Result.success(yuandianLegalService.searchCases(request));
    }

    @PostMapping("/citations/verify")
    @AuditLog(value = "元典法律引证核验", operationType = "VERIFY", logParams = false, logResult = false)
    public Result<Map<String, Object>> verifyCitations(@Valid @RequestBody YuandianCitationCheckRequest request) {
        return Result.success(yuandianLegalService.verifyCitations(request.getContent()));
    }

    @PostMapping("/knowledge/import")
    @AuditLog(value = "导入元典知识", operationType = "IMPORT", logParams = false, logResult = false)
    public Result<KnowledgeArticleVO> importKnowledge(@Valid @RequestBody YuandianImportRequest request) {
        return Result.success("已导入知识库", yuandianLegalService.importToKnowledge(request.getImportToken()));
    }
}
