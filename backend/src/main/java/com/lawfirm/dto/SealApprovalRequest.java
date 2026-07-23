package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class SealApprovalRequest {
    @NotBlank(message = "用印审批标题不能为空")
    @Size(max = 200, message = "用印审批标题不能超过200个字符")
    private String title;

    @NotBlank(message = "用印事由不能为空")
    @Size(max = 5000, message = "用印事由不能超过5000个字符")
    private String content;

    private Long caseId;
    private Long caseDocumentId;
}
