package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
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

    @Column(name = "contract_no", length = 80)
    private String contractNo;

    @Column(name = "invoice_number", unique = true, length = 50)
    private String invoiceNumber;

    @NotBlank(message = "发票类型不能为空")
    @Column(name = "invoice_type", nullable = false, length = 50)
    private String invoiceType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "发票抬头不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "billing_date")
    private LocalDate billingDate;

    @Column(name = "execution_department", length = 100)
    private String executionDepartment;

    @Column(name = "source_user_name", length = 100)
    private String sourceUserName;

    @Column(name = "invoice_content", length = 500)
    private String invoiceContent;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "address_phone", length = 300)
    private String addressPhone;

    @Column(name = "bank_account", length = 300)
    private String bankAccount;

    @Column(name = "applicant_id")
    private Long applicantId;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "invoice_file_path", length = 500)
    private String invoiceFilePath;

    @Column(length = 20)
    private String status = "PENDING";
}
