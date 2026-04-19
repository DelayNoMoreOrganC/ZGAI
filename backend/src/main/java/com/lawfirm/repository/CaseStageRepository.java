package com.lawfirm.repository;

import com.lawfirm.entity.CaseStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 案件阶段Repository
 */
@Repository
public interface CaseStageRepository extends JpaRepository<CaseStage, Long> {

    /**
     * 根据案件ID查询所有阶段
     */
    List<CaseStage> findByCaseIdAndDeletedFalseOrderByStageOrder(Long caseId);

    /**
     * 根据案件ID和阶段名称查询
     */
    Optional<CaseStage> findByCaseIdAndStageNameAndDeletedFalse(Long caseId, String stageName);

    /**
     * 根据案件ID和当前状态查询
     */
    List<CaseStage> findByCaseIdAndStatusAndDeletedFalse(Long caseId, String status);

    /**
     * 查询案件的当前阶段
     */
    @Query("SELECT cs FROM CaseStage cs WHERE cs.caseId = :caseId AND cs.status = 'IN_PROGRESS' AND cs.deleted = false")
    Optional<CaseStage> findCurrentStage(@Param("caseId") Long caseId);

    /**
     * 查询案件的下个阶段
     */
    @Query("SELECT cs FROM CaseStage cs WHERE cs.caseId = :caseId AND cs.stageOrder > :currentOrder AND cs.deleted = false ORDER BY cs.stageOrder ASC")
    Optional<CaseStage> findNextStage(@Param("caseId") Long caseId, @Param("currentOrder") Integer currentOrder);
}
