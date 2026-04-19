package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开票记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "invoice")
public class Invoice extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "invoice_number", unique = true, length = 50)
    private String invoiceNumber;

    @NotBlank(message = "发票类型不能为空")
    @Column(name = "invoice_type", nullable = false, length = 20)
    private String invoiceType;

    @NotNull(message = "发票金额不能为空")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "发票抬头不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @NotNull(message = "开票日期不能为空")
    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    @Column(length = 20)
    private String status = "PENDING";
}
