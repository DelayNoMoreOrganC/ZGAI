package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 收费方式枚举
 */
public enum FeeMethod {
    FIXED("定额"),
    CONTINGENCY("风险代理"),
    HOURLY("计时"),
    PER_ITEM("计件"),
    FREE("免费");

    private final String description;

    FeeMethod(String description) {
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
