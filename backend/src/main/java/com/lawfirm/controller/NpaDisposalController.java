package com.lawfirm.controller;

import com.lawfirm.dto.NpaDisposalPlanDTO;
import com.lawfirm.dto.NpaDisposalResultDTO;
import com.lawfirm.service.NpaDisposalService;
import com.lawfirm.service.NpaDueDiligenceService;
import com.lawfirm.dto.NpaDueDiligenceDTO;
import com.lawfirm.service.NpaPerformanceService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 不良资产处置 Controller（尽调 + 方案 + 结果 + 绩效）
 */
@Slf4j
@RestController
@RequestMapping("/npa")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NpaDisposalController {

    private final NpaDueDiligenceService dueDiligenceService;
    private final NpaDisposalService disposalService;
    private final NpaPerformanceService performanceService;

    // ========== 尽职调查 ==========

    @GetMapping("/assets/{assetId}/diligence")
    public Result<List<NpaDueDiligenceDTO>> listDiligence(@PathVariable Long assetId) {
        return Result.success(dueDiligenceService.listByAsset(assetId));
    }

    @GetMapping("/diligence/{id}")
    public Result<NpaDueDiligenceDTO> getDiligence(@PathVariable Long id) {
        return Result.success(dueDiligenceService.getDueDiligence(id));
    }

    @PostMapping("/diligence")
    public Result<NpaDueDiligenceDTO> createDiligence(@RequestBody NpaDueDiligenceDTO dto) {
        return Result.success("创建成功", dueDiligenceService.createDueDiligence(dto));
    }

    @PutMapping("/diligence/{id}")
    public Result<NpaDueDiligenceDTO> updateDiligence(@PathVariable Long id, @RequestBody NpaDueDiligenceDTO dto) {
        return Result.success("更新成功", dueDiligenceService.updateDueDiligence(id, dto));
    }

    @DeleteMapping("/diligence/{id}")
    public Result<Void> deleteDiligence(@PathVariable Long id) {
        dueDiligenceService.deleteDueDiligence(id);
        return Result.success("删除成功", null);
    }

    // ========== 处置方案 ==========

    @GetMapping("/assets/{assetId}/plans")
    public Result<List<NpaDisposalPlanDTO>> listPlans(@PathVariable Long assetId) {
        return Result.success(disposalService.listPlansByAsset(assetId));
    }

    @GetMapping("/plans/{id}")
    public Result<NpaDisposalPlanDTO> getPlan(@PathVariable Long id) {
        return Result.success(disposalService.getPlan(id));
    }

    @PostMapping("/plans")
    public Result<NpaDisposalPlanDTO> createPlan(@RequestBody NpaDisposalPlanDTO dto) {
        return Result.success("创建成功", disposalService.createPlan(dto));
    }

    @PutMapping("/plans/{id}")
    public Result<NpaDisposalPlanDTO> updatePlan(@PathVariable Long id, @RequestBody NpaDisposalPlanDTO dto) {
        return Result.success("更新成功", disposalService.updatePlan(id, dto));
    }

    @PutMapping("/plans/{id}/approve")
    public Result<NpaDisposalPlanDTO> approvePlan(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String comment = (String) body.getOrDefault("comment", "");
        return Result.success(approved ? "已批准" : "已驳回",
                disposalService.approvePlan(id, comment, approved));
    }

    @DeleteMapping("/plans/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        disposalService.deletePlan(id);
        return Result.success("删除成功", null);
    }

    // ========== 处置结果 ==========

    @GetMapping("/assets/{assetId}/results")
    public Result<List<NpaDisposalResultDTO>> listResults(@PathVariable Long assetId) {
        return Result.success(disposalService.listResultsByAsset(assetId));
    }

    @GetMapping("/results/{id}")
    public Result<NpaDisposalResultDTO> getResult(@PathVariable Long id) {
        return Result.success(disposalService.getResult(id));
    }

    @PostMapping("/results")
    public Result<NpaDisposalResultDTO> createResult(@RequestBody NpaDisposalResultDTO dto) {
        return Result.success("创建成功", disposalService.createResult(dto));
    }

    // ========== 绩效统计 ==========

    @GetMapping("/performance/dashboard")
    public Result<Map<String, Object>> getPerformanceDashboard() {
        return Result.success(performanceService.getPerformanceDashboard());
    }

    @GetMapping("/performance/disposal-stats")
    public Result<Map<String, Object>> getDisposalStats() {
        return Result.success(disposalService.getDisposalStats());
    }
}
