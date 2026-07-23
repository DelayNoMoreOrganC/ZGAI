package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 案件文档实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_document")
public class CaseDocument extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "folder_id")
    private Long folderId;

    @NotBlank(message = "文档名称不能为空")
    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @NotBlank(message = "文档类型不能为空")
    @Column(name = "document_type", nullable = false, length = 20)
    private String documentType;

    @NotBlank(message = "文件路径不能为空")
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "folder_path")
    private String folderPath;

    @Column(name = "version_no")
    private Integer versionNo = 1;

    @NotNull(message = "上传人不能为空")
    @Column(name = "upload_by", nullable = false)
    private Long uploadBy;

    @Column
    private String tags;

    @Column(name = "knowledge_eligible")
    private Boolean knowledgeEligible = false;

    @Column(name = "index_status", length = 30)
    private String indexStatus = "NOT_INDEXED";

    @Lob
    @Column(name = "ocr_result")
    private String ocrResult;

    @Column(name = "content_sha256", length = 64)
    private String contentSha256;
}
