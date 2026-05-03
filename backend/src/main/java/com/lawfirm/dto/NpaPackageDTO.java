package com.lawfirm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产包 DTO
 */
@Data
public class NpaPackageDTO {
    private Long id;
    private String packageName;
    private String packageCode;
    private String bankName;
    private BigDecimal totalAmount;
    private Integer assetCount;
    private LocalDate acquisitionDate;
    private LocalDate deadlineDate;
    private String status;
    private String responsiblePerson;
    private BigDecimal recoveredAmount;
    private BigDecimal costAmount;
    private String description;
    private String createdBy;

    /** 计算字段：回收率 */
    private BigDecimal recoveryRate;

    /** 计算字段：处置天数 */
    private Long disposalDays;
}
