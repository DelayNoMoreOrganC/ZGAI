package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 待办状态枚举
 */
public enum TodoStatus {
    PENDING("待处理"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    OVERDUE("已逾期");

    private final String description;

    TodoStatus(String description) {
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
