package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文书生成请求DTO
 */
@Data
public class DocGenerateRequest {

    @NotNull(message = "案件ID不能为空")
    private Long caseId;

    @NotBlank(message = "文书类型不能为空")
    private String documentType;

    private String customPrompt;

    private String additionalContext;
}
