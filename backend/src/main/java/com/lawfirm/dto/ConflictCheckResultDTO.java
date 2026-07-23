package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户建档前利冲检查结果。
 */
@Data
public class ConflictCheckResultDTO {

    private Long recordId;
    private String reportNo;
    private String clientName;
    private Boolean hasConflict = false;
    private String conflictLevel = "NONE";
    private String conflictDescription;
    private String recommendation;
    private LocalDateTime checkedAt;
    private List<ConflictCheckHitDTO> hits = new ArrayList<>();
    private List<String> similarClientNames = new ArrayList<>();
    private List<Long> conflictCaseIds = new ArrayList<>();
}
