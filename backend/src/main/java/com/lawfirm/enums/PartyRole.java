package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 当事人属性枚举
 */
public enum PartyRole {
    PLAINTIFF("原告"),
    DEFENDANT("被告"),
    THIRD_PARTY("第三人"),
    CO_PLAINTIFF("共同原告"),
    CO_DEFENDANT("共同被告"),
    APPLICANT("申请人"),
    RESPONDENT("被申请人");

    private final String description;

    PartyRole(String description) {
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
