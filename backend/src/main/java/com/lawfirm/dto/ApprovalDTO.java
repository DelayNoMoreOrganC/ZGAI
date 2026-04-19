package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批DTO
 */
@Data
public class ApprovalDTO {
    private Long id;
    private String approvalType;
    private String title;
    private String content;
    private Long caseId;
    private String caseName;
    private Long applicantId;
    private String applicantName;
    private Long currentApproverId;
    private String currentApproverName;
    private String status;
    private String statusDesc;
    private LocalDateTime applyTime;
    private LocalDateTime approvedTime;
    private String approvalNotes;
    private String attachments;
}
