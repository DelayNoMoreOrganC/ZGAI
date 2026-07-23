package com.lawfirm.repository;

import com.lawfirm.entity.KnowledgeImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeImportBatchRepository extends JpaRepository<KnowledgeImportBatch, Long> {
    List<KnowledgeImportBatch> findTop30ByDeletedFalseOrderByCreatedAtDesc();
}
