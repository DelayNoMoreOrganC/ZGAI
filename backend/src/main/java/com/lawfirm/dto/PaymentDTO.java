package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款记录DTO
 */
@Data
public class PaymentDTO {

    private Long id;

    private Long caseId;

    private String caseName;

    @NotNull(message = "收款金额不能为空")
    private BigDecimal paymentAmount;

    @NotBlank(message = "收款方式不能为空")
    private String paymentMethod;

    @NotNull(message = "收款日期不能为空")
    private LocalDate paymentDate;

    private String payer;

    private String bankAccount;

    private String transactionNumber;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
