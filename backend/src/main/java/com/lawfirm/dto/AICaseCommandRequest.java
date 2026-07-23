package com.lawfirm.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AICaseCommandRequest {
    private Long caseId;

    @NotBlank(message = "指令不能为空")
    @Size(max = 4000, message = "指令不能超过4000个字符")
    private String instruction;

    @NotBlank(message = "幂等键不能为空")
    @Size(max = 80, message = "幂等键不能超过80个字符")
    private String idempotencyKey;

    @Size(max = 40, message = "模型类型不能超过40个字符")
    private String providerType;
}
