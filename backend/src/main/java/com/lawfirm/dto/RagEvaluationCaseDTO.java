package com.lawfirm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RagEvaluationCaseDTO {
    private Long id;
    private String name;
    private String question;
    private List<Long> expectedArticleIds;
    private List<Long> forbiddenArticleIds;
    private Boolean enabled;
    private Long createdBy;
    private LocalDateTime createdAt;
}
