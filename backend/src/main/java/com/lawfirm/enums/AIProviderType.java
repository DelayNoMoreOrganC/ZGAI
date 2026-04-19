package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * AI服务提供商类型枚举
 */
public enum AIProviderType {
    DEEPSEEK_API("DeepSeek API"),
    LOCAL_QWEN("本地Qwen"),
    OPENAI_API("OpenAI API"),
    CUSTOM("自定义");

    private final String description;

    AIProviderType(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    public String getCode() {
        return this.name();
    }
}
