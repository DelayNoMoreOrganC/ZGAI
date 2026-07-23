package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "approval_attachment", indexes = {
        @Index(name = "idx_approval_attachment_approval", columnList = "approval_id"),
        @Index(name = "idx_approval_attachment_case_document", columnList = "case_document_id")
})
public class ApprovalAttachment extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "approval_id", nullable = false)
    private Long approvalId;

    @Column(name = "case_document_id")
    private Long caseDocumentId;

    @Column(name = "original_file_name", nullable = false, length = 500)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 1500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 150)
    private String mimeType;

    @Column(name = "content_sha256", length = 64)
    private String contentSha256;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "seal_status", nullable = false, length = 30)
    private String sealStatus = "PENDING";

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
