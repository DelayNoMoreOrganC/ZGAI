package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 财务类型枚举
 */
public enum FinanceType {
    INCOME("收入"),
    EXPENSE("支出");

    private final String description;

    FinanceType(String description) {
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
