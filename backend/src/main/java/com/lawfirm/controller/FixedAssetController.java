package com.lawfirm.controller;

import com.lawfirm.dto.FixedAssetDTO;
import com.lawfirm.service.FixedAssetService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 固定资产管理控制器
 */
@Slf4j
@RestController
@RequestMapping("fixed-assets")
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;

    /**
     * 创建固定资产
     * POST /api/fixed-assets
     */
    @PostMapping
    public Result<FixedAssetDTO> create(@Valid @RequestBody FixedAssetDTO dto) {
        FixedAssetDTO result = fixedAssetService.create(dto);
        return Result.success("创建成功", result);
    }

    /**
     * 更新固定资产
     * PUT /api/fixed-assets/{id}
     */
    @PutMapping("/{id}")
    public Result<FixedAssetDTO> update(@PathVariable Long id,
                                       @Valid @RequestBody FixedAssetDTO dto) {
        FixedAssetDTO result = fixedAssetService.update(id, dto);
        return Result.success("更新成功", result);
    }

    /**
     * 删除固定资产
     * DELETE /api/fixed-assets/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        fixedAssetService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 查询详情
     * GET /api/fixed-assets/{id}
     */
    @GetMapping("/{id}")
    public Result<FixedAssetDTO> getById(@PathVariable Long id) {
        FixedAssetDTO result = fixedAssetService.getById(id);
        return Result.success(result);
    }

    /**
     * 查询所有固定资产
     * GET /api/fixed-assets
     */
    @GetMapping
    public Result<List<FixedAssetDTO>> getAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department) {

        if (category != null && !category.isEmpty()) {
            List<FixedAssetDTO> result = fixedAssetService.getByCategory(category);
            return Result.success(result);
        }

        if (status != null && !status.isEmpty()) {
            List<FixedAssetDTO> result = fixedAssetService.getByUsageStatus(status);
            return Result.success(result);
        }

        if (department != null && !department.isEmpty()) {
            List<FixedAssetDTO> result = fixedAssetService.getByDepartment(department);
            return Result.success(result);
        }

        List<FixedAssetDTO> result = fixedAssetService.getAll();
        return Result.success(result);
    }

    /**
     * 报废资产
     * POST /api/fixed-assets/{id}/scrap
     */
    @PostMapping("/{id}/scrap")
    public Result<FixedAssetDTO> scrap(@PathVariable Long id,
                                     @RequestParam(required = false) String remarks) {
        FixedAssetDTO result = fixedAssetService.scrap(id, remarks);
        return Result.success("报废成功", result);
    }
}
