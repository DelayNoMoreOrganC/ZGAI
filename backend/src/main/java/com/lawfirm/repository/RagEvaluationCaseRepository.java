package com.lawfirm.repository;

import com.lawfirm.entity.RagEvaluationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RagEvaluationCaseRepository extends JpaRepository<RagEvaluationCase, Long> {
    List<RagEvaluationCase> findTop100ByDeletedFalseOrderByCreatedAtDesc();
    List<RagEvaluationCase> findByDeletedFalse();
    List<RagEvaluationCase> findByDeletedFalseAndEnabledTrueOrderByCreatedAtAsc();
}
