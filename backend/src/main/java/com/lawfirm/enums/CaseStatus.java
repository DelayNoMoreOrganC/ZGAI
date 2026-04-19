package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 案件状态枚举
 */
public enum CaseStatus {
    CONSULTATION("咨询"),
    SIGNED("签约"),
    PENDING_FILING("待立案"),
    ACTIVE("审理中"),
    CLOSED("结案"),
    ARCHIVED("归档");

    private final String description;

    CaseStatus(String description) {
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
