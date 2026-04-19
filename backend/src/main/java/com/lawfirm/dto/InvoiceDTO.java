package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开票记录DTO
 */
@Data
public class InvoiceDTO {

    private Long id;

    private Long caseId;

    private String caseName;

    private String invoiceNumber;

    @NotBlank(message = "发票类型不能为空")
    private String invoiceType;

    @NotNull(message = "发票金额不能为空")
    private BigDecimal amount;

    @NotBlank(message = "发票抬头不能为空")
    private String title;

    private String taxNumber;

    @NotNull(message = "开票日期不能为空")
    private LocalDate billingDate;

    private String status = "PENDING";

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
