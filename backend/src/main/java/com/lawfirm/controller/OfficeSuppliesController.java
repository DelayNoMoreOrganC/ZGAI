package com.lawfirm.controller;

import com.lawfirm.dto.OfficeSuppliesDTO;
import com.lawfirm.service.OfficeSuppliesService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 办公用品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("office-supplies")
@RequiredArgsConstructor
public class OfficeSuppliesController {

    private final OfficeSuppliesService officeSuppliesService;

    /**
     * 创建办公用品
     * POST /api/office-supplies
     */
    @PostMapping
    public Result<OfficeSuppliesDTO> create(@Valid @RequestBody OfficeSuppliesDTO dto) {
        OfficeSuppliesDTO result = officeSuppliesService.create(dto);
        return Result.success("创建成功", result);
    }

    /**
     * 更新办公用品
     * PUT /api/office-supplies/{id}
     */
    @PutMapping("/{id}")
    public Result<OfficeSuppliesDTO> update(@PathVariable Long id,
                                           @Valid @RequestBody OfficeSuppliesDTO dto) {
        OfficeSuppliesDTO result = officeSuppliesService.update(id, dto);
        return Result.success("更新成功", result);
    }

    /**
     * 删除办公用品
     * DELETE /api/office-supplies/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        officeSuppliesService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 查询详情
     * GET /api/office-supplies/{id}
     */
    @GetMapping("/{id}")
    public Result<OfficeSuppliesDTO> getById(@PathVariable Long id) {
        OfficeSuppliesDTO result = officeSuppliesService.getById(id);
        return Result.success(result);
    }

    /**
     * 查询所有办公用品
     * GET /api/office-supplies
     */
    @GetMapping
    public Result<List<OfficeSuppliesDTO>> getAll(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            List<OfficeSuppliesDTO> result = officeSuppliesService.getByCategory(category);
            return Result.success(result);
        }

        List<OfficeSuppliesDTO> result = officeSuppliesService.getAll();
        return Result.success(result);
    }

    /**
     * 查询库存不足的物品
     * GET /api/office-supplies/low-stock
     */
    @GetMapping("/low-stock")
    public Result<List<OfficeSuppliesDTO>> getLowStockItems() {
        List<OfficeSuppliesDTO> result = officeSuppliesService.getLowStockItems();
        return Result.success(result);
    }

    /**
     * 入库
     * POST /api/office-supplies/{id}/stock-in
     */
    @PostMapping("/{id}/stock-in")
    public Result<OfficeSuppliesDTO> stockIn(@PathVariable Long id,
                                           @RequestParam Integer quantity) {
        OfficeSuppliesDTO result = officeSuppliesService.stockIn(id, quantity);
        return Result.success("入库成功", result);
    }

    /**
     * 出库
     * POST /api/office-supplies/{id}/stock-out
     */
    @PostMapping("/{id}/stock-out")
    public Result<OfficeSuppliesDTO> stockOut(@PathVariable Long id,
                                           @RequestParam Integer quantity) {
        OfficeSuppliesDTO result = officeSuppliesService.stockOut(id, quantity);
        return Result.success("出库成功", result);
    }
}
