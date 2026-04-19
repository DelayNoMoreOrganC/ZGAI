package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财务记录DTO
 */
@Data
public class FinanceRecordDTO {

    private Long id;

    private Long caseId;

    private String caseName;

    @NotBlank(message = "财务类型不能为空")
    private String financeType;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    private String expenseType;

    private String paymentMethod;

    @NotNull(message = "交易日期不能为空")
    private LocalDate transactionDate;

    private String description;

    private Long operatorId;

    private String operatorName;

    private String receipt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
