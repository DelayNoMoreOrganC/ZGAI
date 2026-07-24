package com.lawfirm.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AICaseCommandResponse {
    private Long commandId;
    private String status;
    private String clarification;
    private Long caseId;
    private String caseName;
    private List<AIActionDTO> actions = new ArrayList<>();
    private List<AICaseCandidateDTO> candidates = new ArrayList<>();
}
