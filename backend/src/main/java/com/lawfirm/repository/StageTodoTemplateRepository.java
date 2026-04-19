package com.lawfirm.repository;

import com.lawfirm.entity.StageTodoTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 案件阶段待办事项模板仓库
 */
@Repository
public interface StageTodoTemplateRepository extends JpaRepository<StageTodoTemplate, Long> {

    /**
     * 根据阶段名称和案件类型查找启用的待办模板
     */
    List<StageTodoTemplate> findByStageNameAndCaseTypeAndIsEnabledAndIsDeletedFalseOrderBySortOrderAsc(
            String stageName, String caseType, Boolean isEnabled
    );

    /**
     * 根据案件类型查找所有启用的待办模板
     */
    List<StageTodoTemplate> findByCaseTypeAndIsEnabledAndIsDeletedFalseOrderByStageNameAscSortOrderAsc(
            String caseType, Boolean isEnabled
    );
}
