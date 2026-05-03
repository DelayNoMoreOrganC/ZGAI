package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单户债权
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "npa_asset")
public class NpaAsset extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "debtor_name", nullable = false, length = 100)
    private String debtorName;

    @Column(name = "debtor_id_number", length = 50)
    private String debtorIdNumber;

    @Column(name = "principal_amount", precision = 18, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", precision = 18, scale = 2)
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "guarantee_type", length = 20)
    private String guaranteeType;

    @Column(columnDefinition = "TEXT")
    private String collateral;

    @Column(name = "guarantee_person", length = 200)
    private String guaranteePerson;

    @Column(name = "lawsuit_status", length = 20)
    private String lawsuitStatus = "NOT_SUED";

    @Column(length = 100)
    private String court;

    @Column(name = "case_number", length = 50)
    private String caseNumber;

    @Column(name = "judge", length = 50)
    private String judge;

    @Column(name = "related_case_id")
    private Long relatedCaseId;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "estimated_recovery", precision = 18, scale = 2)
    private BigDecimal estimatedRecovery;

    @Column(name = "recovered_amount", precision = 18, scale = 2)
    private BigDecimal recoveredAmount = BigDecimal.ZERO;

    @Column(name = "recovery_rate", precision = 5, scale = 2)
    private BigDecimal recoveryRate;

    @Column(name = "risk_level", length = 10)
    private String riskLevel = "MEDIUM";

    @Column(length = 200)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String remark;
}
