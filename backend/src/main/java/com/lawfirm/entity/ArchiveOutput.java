package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "archive_output", indexes = {
        @Index(name = "idx_archive_output_job", columnList = "job_id"),
        @Index(name = "idx_archive_output_case", columnList = "case_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_archive_output_version", columnNames = {"case_id", "version_no"}))
public class ArchiveOutput extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1500)
    private String filePath;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "manifest_file_path", length = 1500)
    private String manifestFilePath;

    @Column(name = "manifest_sha256", length = 64)
    private String manifestSha256;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "source_page_count")
    private Integer sourcePageCount;

    @Column(name = "gap_pages")
    private Integer gapPages = 0;

    @Column(name = "duplicate_pages")
    private Integer duplicatePages = 0;

    @Column(name = "template_version", length = 30)
    private String templateVersion;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
