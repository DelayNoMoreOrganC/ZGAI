package com.lawfirm.repository;

import com.lawfirm.entity.RagEvaluationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RagEvaluationRunRepository extends JpaRepository<RagEvaluationRun, Long> {
    List<RagEvaluationRun> findTop100ByOrderByCreatedAtDesc();
}
