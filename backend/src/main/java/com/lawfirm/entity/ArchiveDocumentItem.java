package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "archive_document_item", indexes = {
        @Index(name = "idx_archive_item_job", columnList = "job_id"),
        @Index(name = "idx_archive_item_document", columnList = "case_document_id")
})
public class ArchiveDocumentItem extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "case_document_id", nullable = false)
    private Long caseDocumentId;

    @Column(name = "original_file_name", nullable = false, length = 500)
    private String originalFileName;

    @Column(name = "catalog_seq")
    private Integer catalogSeq;

    @Column(name = "catalog_name", length = 200)
    private String catalogName;

    @Column(name = "document_type", length = 80)
    private String documentType;

    @Column(name = "included", nullable = false)
    private Boolean included = true;

    @Column(name = "source_page_count")
    private Integer sourcePageCount = 0;

    @Column(name = "output_start_page")
    private Integer outputStartPage;

    @Column(name = "output_end_page")
    private Integer outputEndPage;

    @Column(name = "content_sha256", length = 64)
    private String contentSha256;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "classification_reason", length = 1000)
    private String classificationReason;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
