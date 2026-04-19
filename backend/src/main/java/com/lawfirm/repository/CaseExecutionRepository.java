package com.lawfirm.repository;

import com.lawfirm.entity.CaseExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseExecutionRepository extends JpaRepository<CaseExecution, Long> {
    List<CaseExecution> findByCaseIdAndDeletedFalseOrderByExecutionDateDesc(Long caseId);
}
