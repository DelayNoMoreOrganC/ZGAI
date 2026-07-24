package com.lawfirm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RagEvaluationRunDTO {
    private Long id;
    private Long evaluationCaseId;
    private String caseName;
    private List<Long> retrievedArticleIds;
    private String searchMethod;
    private Boolean top3Hit;
    private Boolean forbiddenHit;
    private Boolean passed;
    private Long durationMs;
    private Long runBy;
    private LocalDateTime createdAt;
}
