package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI聊天请求DTO
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private Long caseId;

    private String sessionId;
}
