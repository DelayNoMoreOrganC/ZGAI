package com.lawfirm.repository;

import com.lawfirm.entity.CaseActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaseActivityRepository extends JpaRepository<CaseActivity, Long> {
    List<CaseActivity> findByCaseIdAndDeletedFalseOrderByOccurredAtDesc(Long caseId);
}
