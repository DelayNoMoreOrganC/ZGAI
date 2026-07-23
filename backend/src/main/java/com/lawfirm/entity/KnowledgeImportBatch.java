package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "knowledge_import_batch")
public class KnowledgeImportBatch extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "item_count")
    private Integer itemCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }
}
