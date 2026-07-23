package com.lawfirm.repository;

import com.lawfirm.entity.CaseDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 案件文档Repository
 */
@Repository
public interface CaseDocumentRepository extends JpaRepository<CaseDocument, Long> {

    boolean existsByCaseIdAndContentSha256AndDeletedFalse(Long caseId, String contentSha256);

    /**
     * 根据案件ID查找文档列表
     */
    List<CaseDocument> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    /**
     * 根据案件ID查找未删除文档列表
     */
    List<CaseDocument> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(Long caseId);

    /**
     * 根据文档类型查找文档
     */
    List<CaseDocument> findByDocumentType(String documentType);

    /**
     * 根据文档类型查找未删除文档
     */
    List<CaseDocument> findByDocumentTypeAndDeletedFalse(String documentType);

    /**
     * 根据案件ID和文档类型查找文档
     */
    List<CaseDocument> findByCaseIdAndDocumentTypeOrderByCreatedAtDesc(Long caseId, String documentType);

    /**
     * 根据案件ID和文档类型查找未删除文档
     */
    List<CaseDocument> findByCaseIdAndDocumentTypeAndDeletedFalseOrderByCreatedAtDesc(Long caseId, String documentType);

    /**
     * 查找所有未删除的文档（搜索优化）
     */
    List<CaseDocument> findByDeletedFalse();
}
