package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 案件等级枚举
 */
public enum CaseLevel {
    IMPORTANT("重要"),
    GENERAL("一般"),
    MINOR("次要");

    private final String description;

    CaseLevel(String description) {
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
