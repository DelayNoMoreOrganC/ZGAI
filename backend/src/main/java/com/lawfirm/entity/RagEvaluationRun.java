package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "rag_evaluation_run", indexes = {
        @Index(name = "idx_rag_evaluation_run_case", columnList = "evaluation_case_id"),
        @Index(name = "idx_rag_evaluation_run_created", columnList = "created_at")
})
public class RagEvaluationRun extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_case_id", nullable = false)
    private Long evaluationCaseId;

    @Column(name = "retrieved_article_ids", length = 2000)
    private String retrievedArticleIds;

    @Column(name = "search_method", nullable = false, length = 30)
    private String searchMethod;

    @Column(name = "top3_hit", nullable = false)
    private Boolean top3Hit;

    @Column(name = "forbidden_hit", nullable = false)
    private Boolean forbiddenHit;

    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "run_by", nullable = false)
    private Long runBy;

}
