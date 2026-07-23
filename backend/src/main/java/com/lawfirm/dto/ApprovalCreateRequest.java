package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 审批创建请求
 */
@Data
public class ApprovalCreateRequest {

    @NotBlank(message = "审批类型不能为空")
    @Size(max = 40, message = "审批类型不能超过40个字符")
    private String approvalType;

    @NotBlank(message = "审批标题不能为空")
    @Size(max = 200, message = "审批标题不能超过200个字符")
    private String title;

    @NotBlank(message = "审批内容不能为空")
    @Size(max = 5000, message = "审批内容不能超过5000个字符")
    private String content;

    private Long caseId;

    @NotNull(message = "当前审批人不能为空")
    private Long currentApproverId;

    @Size(max = 2000, message = "附件信息不能超过2000个字符")
    private String attachments;
}
