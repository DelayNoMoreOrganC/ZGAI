package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LawFirmLetterDTO {
    private Long id;
    private Long caseId;
    private String caseName;
    private String recipient;
    private String clientName;
    private String lawyerNames;
    private String opposingParty;
    private String caseReason;
    private String letterTypeCode;
    private String lawyerContacts;
    private String closingText;
    private LocalDate issueDate;
    private Integer serialNo;
    private String letterNumber;
    private String displayNumber;
    private String status;
    private String statusDesc;
    private Long approvalId;
    private Long finalDocumentId;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private String rejectedReason;
    private Boolean editable;
    private Boolean downloadable;
    private Long lockVersion;
}
