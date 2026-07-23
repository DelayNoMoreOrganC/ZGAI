package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_document_intake", indexes = {
        @Index(name = "idx_document_intake_user", columnList = "upload_by,status"),
        @Index(name = "idx_document_intake_hash", columnList = "content_sha256"),
        @Index(name = "idx_document_intake_expiry", columnList = "expires_at,status")
})
public class AIDocumentIntake extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "temp_path", nullable = false)
    private String tempPath;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Lob
    @Column(name = "extracted_text")
    private String extractedText;

    @Lob
    @Column(name = "analysis_json")
    private String analysisJson;

    @Lob
    @Column(name = "candidates_json")
    private String candidatesJson;

    @Column(name = "suggested_folder", length = 200)
    private String suggestedFolder;

    @Column(name = "suggested_document_type", length = 50)
    private String suggestedDocumentType;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "upload_by", nullable = false)
    private Long uploadBy;

    @Column(name = "confirmed_case_id")
    private Long confirmedCaseId;

    @Column(name = "case_document_id")
    private Long caseDocumentId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
