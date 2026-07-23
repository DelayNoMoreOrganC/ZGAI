package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ApprovalTransferRequest {

    @NotNull(message = "新审批人不能为空")
    private Long newApproverId;

    @Size(max = 2000, message = "转审备注不能超过2000个字符")
    private String comments;
}
