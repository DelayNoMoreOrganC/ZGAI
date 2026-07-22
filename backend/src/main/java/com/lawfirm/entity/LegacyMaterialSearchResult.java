package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 旧资料检索命中项。文件仅保存相对路径，避免数据库和接口暴露存储根目录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "legacy_material_search_result", indexes = {
        @Index(name = "idx_legacy_result_record", columnList = "search_record_id"),
        @Index(name = "idx_legacy_result_case", columnList = "source_case_id")
})
public class LegacyMaterialSearchResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "search_record_id", nullable = false)
    private Long searchRecordId;

    @Column(name = "source_case_id", nullable = false)
    private Long sourceCaseId;

    @Column(name = "relative_path", nullable = false, length = 1200)
    private String relativePath;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
}
