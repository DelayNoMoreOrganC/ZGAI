package com.lawfirm.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "knowledge_import_item", indexes = {
        @Index(name = "idx_knowledge_import_batch", columnList = "batch_id"),
        @Index(name = "idx_knowledge_import_sha", columnList = "content_sha256")
})
public class KnowledgeImportItem extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "batch_id", nullable = false)
    private Long batchId;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(length = 300)
    private String title;
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;
    @Column(name = "source_relative_path", length = 1000)
    private String sourceRelativePath;
    @Column(name = "original_file_name", length = 500)
    private String originalFileName;
    @JsonIgnore
    @Column(name = "source_absolute_path", length = 1500)
    private String sourceAbsolutePath;
    @JsonIgnore
    @Column(name = "staged_path", length = 1500)
    private String stagedPath;
    @Column(name = "content_sha256", length = 64)
    private String contentSha256;
    @Column(name = "issuing_authority", length = 200)
    private String issuingAuthority;
    @Column(name = "document_number", length = 100)
    private String documentNumber;
    @Column(name = "published_date")
    private LocalDate publishedDate;
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    @Column(name = "validity_status", length = 20)
    private String validityStatus = "UNKNOWN";
    @Column(name = "article_id")
    private Long articleId;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "collected_at")
    private LocalDateTime collectedAt = LocalDateTime.now();
}
