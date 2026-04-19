package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 当事人类型枚举
 */
public enum PartyType {
    INDIVIDUAL("个人"),
    ORGANIZATION("单位");

    private final String description;

    PartyType(String description) {
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
