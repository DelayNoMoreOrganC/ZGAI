package com.lawfirm.controller;

import com.lawfirm.dto.NpaAssetDTO;
import com.lawfirm.service.NpaAssetService;
import com.lawfirm.util.PageResult;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 债权管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/npa/assets")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NpaAssetController {

    private final NpaAssetService npaAssetService;

    /**
     * 按资产包分页查询债权
     * GET /api/npa/assets?packageId=1&page=0&size=10
     */
    @GetMapping
    public Result<PageResult<NpaAssetDTO>> listAssets(
            @RequestParam(required = false) Long packageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NpaAssetDTO> result;
        if (packageId != null) {
            result = npaAssetService.listAssetsByPackage(packageId, page, size);
        } else {
            result = npaAssetService.listAllAssets(page, size);
        }
        return Result.success(new PageResult<>(result));
    }

    /**
     * 获取债权详情
     * GET /api/npa/assets/{id}
     */
    @GetMapping("/{id}")
    public Result<NpaAssetDTO> getAsset(@PathVariable Long id) {
        return Result.success(npaAssetService.getAsset(id));
    }

    /**
     * 创建债权
     * POST /api/npa/assets
     */
    @PostMapping
    public Result<NpaAssetDTO> createAsset(@RequestBody NpaAssetDTO dto) {
        return Result.success("创建成功", npaAssetService.createAsset(dto));
    }

    /**
     * 更新债权
     * PUT /api/npa/assets/{id}
     */
    @PutMapping("/{id}")
    public Result<NpaAssetDTO> updateAsset(@PathVariable Long id, @RequestBody NpaAssetDTO dto) {
        return Result.success("更新成功", npaAssetService.updateAsset(id, dto));
    }

    /**
     * 录入回收金额
     * PUT /api/npa/assets/{id}/recovery
     */
    @PutMapping("/{id}/recovery")
    public Result<NpaAssetDTO> recordRecovery(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("recoveredAmount");
        if (amount == null) {
            return Result.error("请填写回收金额");
        }
        return Result.success("回收金额已更新", npaAssetService.recordRecovery(id, amount));
    }

    /**
     * 删除债权
     * DELETE /api/npa/assets/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAsset(@PathVariable Long id) {
        npaAssetService.deleteAsset(id);
        return Result.success("删除成功", null);
    }

    /**
     * 搜索债务人
     * GET /api/npa/assets/search?keyword=张三
     */
    @GetMapping("/search")
    public Result<List<NpaAssetDTO>> searchDebtor(@RequestParam String keyword) {
        return Result.success(npaAssetService.searchByDebtor(keyword));
    }
}
