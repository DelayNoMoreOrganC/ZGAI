package com.lawfirm.repository;

import com.lawfirm.entity.ArchiveDocumentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchiveDocumentItemRepository extends JpaRepository<ArchiveDocumentItem, Long> {
    List<ArchiveDocumentItem> findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(Long jobId);
}
