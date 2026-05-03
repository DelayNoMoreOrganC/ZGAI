package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 处置方案
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "npa_disposal_plan")
public class NpaDisposalPlan extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "disposal_method", nullable = false, length = 30)
    private String disposalMethod;

    @Column(name = "target_amount", precision = 18, scale = 2)
    private BigDecimal targetAmount;

    @Column
    private LocalDate deadline;

    @Column(name = "responsible_person", length = 50)
    private String responsiblePerson;

    @Column(name = "plan_detail", columnDefinition = "TEXT")
    private String planDetail;

    @Column(length = 20)
    private String status = "PENDING_REVIEW";

    @Column(name = "approval_comment", columnDefinition = "TEXT")
    private String approvalComment;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;
}
