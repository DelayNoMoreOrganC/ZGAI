package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 办公用品DTO
 */
@Data
public class OfficeSuppliesDTO {

    private Long id;

    @NotBlank(message = "物品名称不能为空")
    private String name;

    @NotBlank(message = "物品类别不能为空")
    private String category;

    private String specification;

    private String unit;

    @NotNull(message = "库存数量不能为空")
    private Integer stockQuantity = 0;

    private Integer minStock = 5;

    private BigDecimal unitPrice;

    private String supplier;

    private String location;

    private String status;

    private LocalDate lastStockInDate;

    private String remarks;

    private LocalDate createTime;
}
