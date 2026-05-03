package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 不良资产尽职调查
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "npa_due_diligence")
public class NpaDueDiligence extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "investigation_date")
    private LocalDate investigationDate;

    @Column(name = "investigator", length = 50)
    private String investigator;

    @Column(name = "property_status", columnDefinition = "TEXT")
    private String propertyStatus;

    @Column(name = "business_status", columnDefinition = "TEXT")
    private String businessStatus;

    @Column(name = "contact_history", columnDefinition = "TEXT")
    private String contactHistory;

    @Column(name = "recovery_analysis", columnDefinition = "TEXT")
    private String recoveryAnalysis;

    @Column(name = "recovery_estimate", precision = 18, scale = 2)
    private BigDecimal recoveryEstimate;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Column(name = "report_content", columnDefinition = "TEXT")
    private String reportContent;

    @Column(name = "attachments", length = 500)
    private String attachments;

    @Column(name = "ai_generated")
    private Boolean aiGenerated = false;

    @Column(columnDefinition = "TEXT")
    private String conclusion;
}
