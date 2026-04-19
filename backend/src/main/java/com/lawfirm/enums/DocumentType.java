package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文档类型枚举
 */
public enum DocumentType {
    COMPLAINT("起诉状"),
    DEFENSE("答辩状"),
    PLAINTIFF_EVIDENCE("原告证据"),
    DEFENDANT_EVIDENCE("被告证据"),
    COURT_DOCUMENT("法院文书"),
    BRIEF("代理词"),
    JUDGMENT("判决书"),
    OTHER("其他");

    private final String description;

    DocumentType(String description) {
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
