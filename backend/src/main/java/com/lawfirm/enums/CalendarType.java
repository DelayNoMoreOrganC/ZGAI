package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 日程类型枚举
 */
public enum CalendarType {
    HEARING("开庭/听证"),
    DEADLINE("审限届满"),
    FILING("立案"),
    MEDIATION("调解/和解"),
    EVIDENCE_DEADLINE("举证截止"),
    MEETING("会议"),
    OTHER("其他");

    private final String description;

    CalendarType(String description) {
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
