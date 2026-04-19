package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产DTO
 */
@Data
public class FixedAssetDTO {

    private Long id;

    @NotBlank(message = "资产名称不能为空")
    private String assetName;

    @NotBlank(message = "资产类别不能为空")
    private String assetCategory;

    private String assetCode;

    private String specification;

    private LocalDate purchaseDate;

    private BigDecimal purchasePrice;

    private BigDecimal currentValue;

    private String department;

    private String custodian;

    private String location;

    private String usageStatus;

    private Integer depreciationYears;

    private String depreciationMethod;

    private String remarks;
}
