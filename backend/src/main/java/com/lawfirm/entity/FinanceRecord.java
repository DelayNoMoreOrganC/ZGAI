package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "finance_record")
public class FinanceRecord extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id")
    private Long caseId;

    @NotBlank(message = "财务类型不能为空")
    @Column(name = "finance_type", nullable = false, length = 20)
    private String financeType;

    @NotNull(message = "金额不能为空")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_type", length = 50)
    private String expenseType;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @NotNull(message = "交易日期不能为空")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column
    private String description;

    @NotNull(message = "操作人不能为空")
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column
    private String receipt;
}
