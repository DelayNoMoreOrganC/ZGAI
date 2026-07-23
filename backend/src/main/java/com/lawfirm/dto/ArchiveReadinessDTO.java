package com.lawfirm.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ArchiveReadinessDTO {
    private Long caseId;
    private String caseStatus;
    private String caseType;
    private boolean ready;
    private boolean canStart;
    private int documentCount;
    private List<String> missingCritical = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
