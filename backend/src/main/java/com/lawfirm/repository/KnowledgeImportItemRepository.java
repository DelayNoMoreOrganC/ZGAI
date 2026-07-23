package com.lawfirm.repository;

import com.lawfirm.entity.KnowledgeImportItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeImportItemRepository extends JpaRepository<KnowledgeImportItem, Long> {
    List<KnowledgeImportItem> findByBatchIdAndDeletedFalseOrderByIdAsc(Long batchId);
    Optional<KnowledgeImportItem> findFirstByContentSha256AndArticleIdIsNotNullAndDeletedFalse(String sha256);
    Optional<KnowledgeImportItem> findFirstByArticleIdAndDeletedFalse(Long articleId);
    boolean existsBySourceUrlAndDeletedFalse(String sourceUrl);
}
