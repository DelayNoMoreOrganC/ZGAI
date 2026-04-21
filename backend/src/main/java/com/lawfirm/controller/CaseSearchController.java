package com.lawfirm.controller;

import com.lawfirm.dto.CaseSearchRequest;
import com.lawfirm.service.CaseSearchService;
import com.lawfirm.util.Result;
import com.lawfirm.vo.CaseSearchResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 类案检索Controller
 *
 * 功能：根据案件特征检索相似案例
 * 优化点：多维度相似度计算（案由、争议金额、案件类型、法院层级）
 */
@Slf4j
@RestController
@RequestMapping("/case-search")
@RequiredArgsConstructor
@Tag(name = "类案检索", description = "根据案件特征检索相似案例")
public class CaseSearchController {

    private final CaseSearchService caseSearchService;

    /**
     * 智能类案检索
     * POST /api/case-search/similar
     *
     * @param request 检索条件（案件特征）
     * @return 相似案例列表（按相似度降序）
     */
    @PostMapping("/similar")
    @Operation(summary = "检索相似案例")
    public Result<List<CaseSearchResultVO>> searchSimilarCases(@Valid @RequestBody CaseSearchRequest request) {
        try {
            log.info("类案检索请求: 案由={}, 类型={}, 争议金额={}, 法院={}",
                    request.getCaseReason(),
                    request.getCaseType(),
                    request.getAmount(),
                    request.getCourt());

            List<CaseSearchResultVO> results = caseSearchService.searchSimilarCases(request);

            log.info("类案检索完成: 找到{}个相似案例", results.size());

            return Result.success(results);

        } catch (Exception e) {
            log.error("类案检索失败", e);
            return Result.error("检索失败: " + e.getMessage());
        }
    }

    /**
     * 根据案件ID检索相似案例
     * GET /api/case-search/similar/{caseId}
     *
     * @param caseId 目标案件ID
     * @param limit 返回结果数量限制
     * @return 相似案例列表
     */
    @GetMapping("/similar/{caseId}")
    @Operation(summary = "根据案件ID检索相似案例")
    public Result<List<CaseSearchResultVO>> searchSimilarByCaseId(
            @PathVariable Long caseId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("根据案件ID检索相似案例: caseId={}, limit={}", caseId, limit);

            List<CaseSearchResultVO> results = caseSearchService.searchSimilarByCaseId(caseId, limit);

            log.info("检索完成: 找到{}个相似案例", results.size());

            return Result.success(results);

        } catch (Exception e) {
            log.error("根据案件ID检索失败", e);
            return Result.error("检索失败: " + e.getMessage());
        }
    }
}