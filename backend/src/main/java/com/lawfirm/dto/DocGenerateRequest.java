package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 文书生成请求DTO
 */
@Data
public class DocGenerateRequest {

    @NotNull(message = "案件ID不能为空")
    private Long caseId;

    @NotBlank(message = "文书类型不能为空")
    @Size(max = 40, message = "文书类型不能超过40个字符")
    private String documentType;

    @Size(max = 10000, message = "自定义要求不能超过10000个字符")
    private String customPrompt;

    @Size(max = 20000, message = "补充材料不能超过20000个字符")
    private String additionalContext;

    @Size(max = 40, message = "模型类型不能超过40个字符")
    private String providerType;
}
