package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 客户类型枚举
 */
public enum ClientType {
    INDIVIDUAL("个人"),
    COMPANY("企业"),
    GOVERNMENT("政府"),
    OTHER("其他");

    private final String description;

    ClientType(String description) {
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
