package com.lawfirm.repository;

import com.lawfirm.entity.CaseClosureDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseClosureDocumentRepository extends JpaRepository<CaseClosureDocument, Long> {
    List<CaseClosureDocument> findByClosureRequestIdAndDeletedFalseOrderByIdAsc(Long closureRequestId);
}
