package com.lawfirm.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class DocumentMatchCandidateDTO {
    private Long caseId;
    private String caseName;
    private String caseNumber;
    private String courtCaseNumber;
    private Integer score;
    private List<String> reasons = new ArrayList<>();
}
