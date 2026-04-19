package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 审批状态枚举
 */
public enum ApprovalStatus {
    PENDING("待审批"),
    APPROVED("已同意"),
    REJECTED("已驳回"),
    TRANSFERRED("已转审"),
    WITHDRAWN("已撤回");

    private final String description;

    ApprovalStatus(String description) {
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
