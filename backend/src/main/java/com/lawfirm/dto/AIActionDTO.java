package com.lawfirm.dto;

import lombok.Data;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AIActionDTO {
    private String actionType;
    private String riskLevel;
    private Long caseId;
    private Map<String, Object> payload = new LinkedHashMap<>();
    private Double confidence;
    private Boolean requiresConfirmation;
    private String reason;
}
