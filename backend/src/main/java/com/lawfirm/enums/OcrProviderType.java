package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * OCR服务提供商类型枚举
 */
public enum OcrProviderType {
    PADDLE_OCR("PaddleOCR"),
    TENCENT_OCR("腾讯OCR"),
    ALIYUN_OCR("阿里OCR");

    private final String description;

    OcrProviderType(String description) {
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
