package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 客户状态枚举
 */
public enum ClientStatus {
    ACTIVE("活跃"),
    INACTIVE("非活跃"),
    POTENTIAL("潜在"),
    BLACKLIST("黑名单");

    private final String description;

    ClientStatus(String description) {
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
