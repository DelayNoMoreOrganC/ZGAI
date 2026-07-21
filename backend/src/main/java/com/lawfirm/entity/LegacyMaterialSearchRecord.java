package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 旧系统资料检索留痕。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "legacy_material_search_record", indexes = {
        @Index(name = "idx_legacy_search_user", columnList = "searched_by"),
        @Index(name = "idx_legacy_search_keyword", columnList = "keyword"),
        @Index(name = "idx_legacy_search_created", columnList = "created_at")
})
public class LegacyMaterialSearchRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", length = 200)
    private String keyword;

    @Column(name = "query_params", length = 2000)
    private String queryParams;

    @Column(name = "searched_by")
    private Long searchedBy;

    @Column(name = "result_count")
    private Integer resultCount = 0;

    @Column(name = "archive_path_configured")
    private Boolean archivePathConfigured = false;
}
