package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 案件状态枚举。
 */
public enum CaseStatus {
    CONSULTATION("咨询"),
    PENDING_APPROVAL("待审批"),
    FILING_REJECTED("立案驳回"),
    SIGNED("签约"),
    PENDING_FILING("待立案"),
    ACTIVE("审理中"),
    CLOSED("结案", true),
    ARCHIVED("归档", true);

    private final String description;
    private final boolean terminal;

    CaseStatus(String description) {
        this.description = description;
        this.terminal = false;
    }

    CaseStatus(String description, boolean terminal) {
        this.description = description;
        this.terminal = terminal;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    public String getCode() {
        return this.name();
    }

    public boolean isTerminal() {
        return terminal;
    }

    /**
     * 待审批、结案和归档案件的案卷只读。
     */
    public boolean isDocumentLocked() {
        return this == PENDING_APPROVAL || terminal;
    }

    public static CaseStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        try {
            return CaseStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
