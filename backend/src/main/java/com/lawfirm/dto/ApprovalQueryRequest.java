package com.lawfirm.dto;

import lombok.Data;

/**
 * 审批查询请求
 */
@Data
public class ApprovalQueryRequest {
    private Integer page = 1;
    private Integer size = 20;
    private String sortField = "applyTime";
    private String sortDirection = "DESC";

    private String approvalType;
    private String status;
    private Long applicantId;
    private Long currentApproverId;
    private Long caseId;
    private String keyword;
}
