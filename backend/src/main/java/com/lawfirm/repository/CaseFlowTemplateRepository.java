package com.lawfirm.repository;

import com.lawfirm.entity.CaseFlowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 案件流程模板Repository
 */
@Repository
public interface CaseFlowTemplateRepository extends JpaRepository<CaseFlowTemplate, Long> {

    /**
     * 根据案件类型查询启用的流程模板
     */
    List<CaseFlowTemplate> findByCaseTypeAndEnabledTrueAndDeletedFalseOrderBySortOrderAsc(String caseType);

    /**
     * 根据案件类型查询系统预置模板
     */
    Optional<CaseFlowTemplate> findByCaseTypeAndIsSystemTrueAndDeletedFalse(String caseType);

    /**
     * 查询所有启用的模板
     */
    List<CaseFlowTemplate> findByEnabledTrueAndDeletedFalseOrderBySortOrderAsc();

    /**
     * 查询所有系统预置模板
     */
    List<CaseFlowTemplate> findByIsSystemTrueAndDeletedFalseOrderByCaseTypeAsc();
}
