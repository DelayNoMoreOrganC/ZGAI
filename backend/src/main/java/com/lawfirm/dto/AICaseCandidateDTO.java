package com.lawfirm.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AICaseCandidateDTO {
    private Long caseId;
    private String caseName;
    private String caseNumber;
    private String courtCaseNumber;
    private Integer score;
    private Boolean canEdit;
    private List<String> reasons = new ArrayList<>();
}
