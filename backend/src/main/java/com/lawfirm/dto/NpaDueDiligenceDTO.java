package com.lawfirm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class NpaDueDiligenceDTO {
    private Long id;
    private Long assetId;
    private String debtorName;
    private LocalDate investigationDate;
    private String investigator;
    private String propertyStatus;
    private String businessStatus;
    private String contactHistory;
    private String recoveryAnalysis;
    private BigDecimal recoveryEstimate;
    private String riskLevel;
    private String reportContent;
    private String attachments;
    private Boolean aiGenerated;
    private String conclusion;
}
