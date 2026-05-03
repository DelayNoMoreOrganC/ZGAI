package com.lawfirm.controller;

import com.lawfirm.dto.NpaPackageDTO;
import com.lawfirm.service.NpaPackageService;
import com.lawfirm.util.PageResult;
import com.lawfirm.util.Result;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 不良资产包管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/npa/packages")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NpaPackageController {

    private final NpaPackageService npaPackageService;
    private final SecurityUtils securityUtils;

    /**
     * 分页查询资产包
     * GET /api/npa/packages?page=0&size=10
     */
    @GetMapping
    public Result<PageResult<NpaPackageDTO>> listPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NpaPackageDTO> result = npaPackageService.listPackages(page, size);
        return Result.success(new PageResult<>(result));
    }

    /**
     * 获取资产包详情
     * GET /api/npa/packages/{id}
     */
    @GetMapping("/{id}")
    public Result<NpaPackageDTO> getPackage(@PathVariable Long id) {
        return Result.success(npaPackageService.getPackage(id));
    }

    /**
     * 创建资产包
     * POST /api/npa/packages
     */
    @PostMapping
    public Result<NpaPackageDTO> createPackage(@RequestBody NpaPackageDTO dto) {
        dto.setCreatedBy(securityUtils.getCurrentUsername());
        return Result.success("创建成功", npaPackageService.createPackage(dto));
    }

    /**
     * 更新资产包
     * PUT /api/npa/packages/{id}
     */
    @PutMapping("/{id}")
    public Result<NpaPackageDTO> updatePackage(@PathVariable Long id, @RequestBody NpaPackageDTO dto) {
        return Result.success("更新成功", npaPackageService.updatePackage(id, dto));
    }

    /**
     * 删除资产包（逻辑删除）
     * DELETE /api/npa/packages/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePackage(@PathVariable Long id) {
        npaPackageService.deletePackage(id);
        return Result.success("删除成功", null);
    }

    /**
     * 全局概览统计
     * GET /api/npa/packages/stats/overview
     */
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(npaPackageService.getOverviewStats());
    }
}
