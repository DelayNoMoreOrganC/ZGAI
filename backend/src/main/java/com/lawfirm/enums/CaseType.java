package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 案件类型枚举
 */
public enum CaseType {
    CIVIL("民事诉讼"),
    COMMERCIAL("商事（历史类型）"),
    ARBITRATION("商事仲裁"),
    CRIMINAL("刑事"),
    ADMINISTRATIVE("行政"),
    NON_LITIGATION("非诉专项"),
    CONSULTANT("法律顾问");

    private final String description;

    CaseType(String description) {
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
