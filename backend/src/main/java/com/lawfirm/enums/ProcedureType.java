package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 程序类型枚举
 */
public enum ProcedureType {
    FIRST_INSTANCE("一审"),
    SECOND_INSTANCE("二审"),
    RETRIAL("再审"),
    EXECUTION("执行"),
    OTHER("其他");

    private final String description;

    ProcedureType(String description) {
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
