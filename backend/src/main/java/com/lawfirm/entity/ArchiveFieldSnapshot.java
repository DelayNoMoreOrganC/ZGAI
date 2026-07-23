package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "archive_field_snapshot", indexes = @Index(name = "idx_archive_field_job", columnList = "job_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_archive_field_key", columnNames = {"job_id", "field_key"}))
public class ArchiveFieldSnapshot extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Lob
    @Column(name = "field_value")
    private String fieldValue;

    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "extraction_reason", length = 1000)
    private String extractionReason;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
}
