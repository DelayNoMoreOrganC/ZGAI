package com.lawfirm.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CaseActivityDTO {
    private Long id;
    private Long caseId;
    private String activityType;
    private String title;
    private String content;
    private LocalDateTime occurredAt;
    private String sourceType;
    private Long sourceId;
    private Long operatorId;
    private String procedureStage;
    private String metadataJson;
    private LocalDateTime createdAt;
}
