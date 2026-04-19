package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * AI功能类型枚举
 */
public enum AIFunctionType {
    OCR_RECOGNITION("OCR识别"),
    AUTO_FILL("智能填充"),
    DOCUMENT_GENERATION("文书生成"),
    LEGAL_QA("法律问答"),
    CASE_ANALYSIS("案情分析"),
    OTHER("其他");

    private final String description;

    AIFunctionType(String description) {
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
