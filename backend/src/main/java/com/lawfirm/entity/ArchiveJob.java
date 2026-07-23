package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "archive_job", indexes = {
        @Index(name = "idx_archive_job_case", columnList = "case_id"),
        @Index(name = "idx_archive_job_status", columnList = "status")
}, uniqueConstraints = @UniqueConstraint(name = "uk_archive_job_idempotency", columnNames = "idempotency_key"))
public class ArchiveJob extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "template_version", nullable = false, length = 30)
    private String templateVersion = "CIVIL_V1";

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "progress")
    private Integer progress = 0;

    @Column(name = "current_stage", length = 100)
    private String currentStage;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "review_reason", length = 1000)
    private String reviewReason;

    @Column(name = "exception_reason", length = 1000)
    private String exceptionReason;

    @Column(name = "correction_reason", length = 1000)
    private String correctionReason;

    @Column(name = "model_provider", length = 30)
    private String modelProvider = "LM_STUDIO";

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion = "ARCHIVE_CIVIL_V1";
}
