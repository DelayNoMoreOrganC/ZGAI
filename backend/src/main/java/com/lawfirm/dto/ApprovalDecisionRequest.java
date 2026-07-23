package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class ApprovalDecisionRequest {

    @Size(max = 2000, message = "审批意见不能超过2000个字符")
    private String comments;
}
