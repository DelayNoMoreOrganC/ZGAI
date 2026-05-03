package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 处置结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "npa_disposal_result")
public class NpaDisposalResult extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "disposal_date")
    private LocalDate disposalDate;

    @Column(name = "actual_recovery", precision = 18, scale = 2)
    private BigDecimal actualRecovery;

    @Column(name = "recovery_rate", precision = 5, scale = 2)
    private BigDecimal recoveryRate;

    @Column(name = "cost_amount", precision = 18, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;

    @Column(name = "net_recovery", precision = 18, scale = 2)
    private BigDecimal netRecovery;

    @Column(name = "recovery_method", length = 20)
    private String recoveryMethod;

    @Column(name = "recovery_account", length = 100)
    private String recoveryAccount;

    @Column(name = "result_description", columnDefinition = "TEXT")
    private String resultDescription;

    @Column(length = 20)
    private String status = "IN_PROGRESS";

    @Column(length = 500)
    private String attachments;
}
