package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CaseClosureRequestDTO {
    private Long id;
    private Long caseId;
    private Long approvalId;
    private Long applicantId;
    private String applicantName;
    private String closureType;
    private String closureTypeDesc;
    private String caseOutcome;
    private String closureSummary;
    private String feeStatus;
    private String feeStatusDesc;
    private String clientDeliveryStatus;
    private String clientDeliveryStatusDesc;
    private String clientDeliveryNotes;
    private Boolean documentsConfirmed;
    private String status;
    private LocalDateTime requestedAt;
    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
    private List<CaseClosureDocumentDTO> basisDocuments;
}
