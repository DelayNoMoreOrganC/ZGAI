package com.lawfirm.repository;

import com.lawfirm.entity.CaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 案件记录Repository
 */
@Repository
public interface CaseRecordRepository extends JpaRepository<CaseRecord, Long> {

    /**
     * 根据案件ID查找记录列表
     */
    List<CaseRecord> findByCaseIdOrderByRecordDateDesc(Long caseId);

    /**
     * 根据日期范围查找记录
     */
    List<CaseRecord> findByRecordDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 根据案件ID查找未删除的记录列表（按记录日期和创建时间倒序）
     */
    List<CaseRecord> findByCaseIdAndDeletedFalseOrderByRecordDateDescCreatedAtDesc(Long caseId);

    /**
     * 根据案件ID和阶段查找未删除的记录
     */
    List<CaseRecord> findByCaseIdAndStageAndDeletedFalse(Long caseId, String stage);
}
