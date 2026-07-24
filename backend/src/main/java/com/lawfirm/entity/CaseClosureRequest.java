package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_closure_request")
public class CaseClosureRequest extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "approval_id", nullable = false, unique = true)
    private Long approvalId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "review_todo_id")
    private Long reviewTodoId;

    @Column(name = "closure_type", nullable = false, length = 40)
    private String closureType;

    @Column(name = "case_outcome", nullable = false, length = 1000)
    private String caseOutcome;

    @Column(name = "closure_summary", nullable = false, length = 5000)
    private String closureSummary;

    @Column(name = "fee_status", nullable = false, length = 40)
    private String feeStatus;

    @Column(name = "client_delivery_status", nullable = false, length = 40)
    private String clientDeliveryStatus;

    @Column(name = "client_delivery_notes", length = 1000)
    private String clientDeliveryNotes;

    @Column(name = "documents_confirmed", nullable = false)
    private Boolean documentsConfirmed = false;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 2000)
    private String reviewNotes;
}
