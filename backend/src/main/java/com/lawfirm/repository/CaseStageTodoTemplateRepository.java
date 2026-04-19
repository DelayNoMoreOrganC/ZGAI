package com.lawfirm.repository;

import com.lawfirm.entity.CaseStageTodoTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 案件阶段待办模板Repository
 */
@Repository
public interface CaseStageTodoTemplateRepository extends JpaRepository<CaseStageTodoTemplate, Long> {

    /**
     * 根据流程模板ID查询所有待办模板
     */
    List<CaseStageTodoTemplate> findByFlowTemplateIdAndDeletedFalseOrderByStageOrderAscSortOrderAsc(Long flowTemplateId);

    /**
     * 根据流程模板ID和阶段名称查询待办模板
     */
    List<CaseStageTodoTemplate> findByFlowTemplateIdAndStageNameAndDeletedFalseOrderBySortOrderAsc(
            Long flowTemplateId, String stageName);
}
