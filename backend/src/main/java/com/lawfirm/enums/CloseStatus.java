package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 结案状态枚举
 */
public enum CloseStatus {
    FULLY_ACHIEVED("达成诉求"),
    PARTIALLY_ACHIEVED("部分达成"),
    NOT_ACHIEVED("未达成"),
    NO_COMMISSION("未委托"),
    TERMINATED("终止"),
    OTHER("其他");

    private final String description;

    CloseStatus(String description) {
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
