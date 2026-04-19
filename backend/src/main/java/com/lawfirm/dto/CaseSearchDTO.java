package com.lawfirm.dto;

import lombok.Data;

@Data
public class CaseSearchDTO {
    private Long id;
    private String caseNumber;
    private String title;
    private String caseType;
    private String court;
    private String judgmentDate;
    private String caseBrief;
}
