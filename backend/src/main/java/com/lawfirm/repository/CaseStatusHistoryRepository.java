package com.lawfirm.repository;

import com.lawfirm.entity.CaseStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 案件状态历史记录仓库接口
 */
@Repository
public interface CaseStatusHistoryRepository extends JpaRepository<CaseStatusHistory, Long> {

    /**
     * 查询指定案件的状态历史
     */
    @Query("SELECT h FROM CaseStatusHistory h WHERE h.caseId = :caseId ORDER BY h.changeTime DESC")
    List<CaseStatusHistory> findByCaseId(@Param("caseId") Long caseId);

    /**
     * 统计案件状态变更次数
     */
    @Query("SELECT COUNT(h) FROM CaseStatusHistory h WHERE h.caseId = :caseId")
    Long countByCaseId(@Param("caseId") Long caseId);
}
