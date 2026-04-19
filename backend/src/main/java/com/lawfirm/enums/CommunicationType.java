package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 沟通方式枚举
 */
public enum CommunicationType {
    PHONE("电话"),
    EMAIL("邮件"),
    VISIT("拜访"),
    WECHAT("微信"),
    MEETING("会议"),
    OTHER("其他");

    private final String description;

    CommunicationType(String description) {
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
