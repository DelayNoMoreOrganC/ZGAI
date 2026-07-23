package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 附条件利冲审查所依据的书面豁免或风险处置原件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "conflict_waiver_attachment", indexes = {
        @Index(name = "idx_conflict_waiver_record", columnList = "conflict_check_record_id"),
        @Index(name = "idx_conflict_waiver_case", columnList = "case_id"),
        @Index(name = "idx_conflict_waiver_hash", columnList = "content_sha256")
})
public class ConflictWaiverAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conflict_check_record_id", nullable = false)
    private Long conflictCheckRecordId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "archived_document_id")
    private Long archivedDocumentId;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}
