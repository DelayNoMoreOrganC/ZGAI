package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 待办优先级枚举
 */
public enum TodoPriority {
    URGENT("紧急"),
    IMPORTANT("重要"),
    NORMAL("普通");

    private final String description;

    TodoPriority(String description) {
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
