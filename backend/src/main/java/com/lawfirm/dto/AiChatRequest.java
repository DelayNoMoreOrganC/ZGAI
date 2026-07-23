package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * AI聊天请求DTO
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 20000, message = "消息内容不能超过20000个字符")
    private String message;

    private Long caseId;

    @Size(max = 128, message = "会话标识不能超过128个字符")
    private String sessionId;

    @Size(max = 40, message = "模型类型不能超过40个字符")
    private String providerType;
}
