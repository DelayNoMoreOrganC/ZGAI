package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 案件文件夹/目录模板实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "document_folder", indexes = {
    @Index(name = "idx_document_folder_case", columnList = "case_id"),
    @Index(name = "idx_document_folder_type", columnList = "folder_type"),
    @Index(name = "idx_document_folder_active", columnList = "active")
})
public class DocumentFolder extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 空表示全局目录模板；非空表示某个案件的实际目录。
     */
    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "parent_id")
    private Long parentId;

    @NotBlank(message = "目录编码不能为空")
    @Column(name = "folder_code", nullable = false, length = 50)
    private String folderCode;

    @NotBlank(message = "目录名称不能为空")
    @Column(name = "folder_name", nullable = false, length = 100)
    private String folderName;

    @Column(name = "folder_path", nullable = false, length = 300)
    private String folderPath;

    /**
     * TEMPLATE: 全局模板；CASE: 案件实际目录。
     */
    @Column(name = "folder_type", nullable = false, length = 20)
    private String folderType = "TEMPLATE";

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "system_default")
    private Boolean systemDefault = false;
}
