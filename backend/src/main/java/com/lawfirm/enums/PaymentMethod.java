package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 支付方式枚举
 */
public enum PaymentMethod {
    CASH("现金"),
    BANK_TRANSFER("银行转账"),
    CHECK("支票"),
    ALIPAY("支付宝"),
    WECHAT_PAY("微信支付"),
    OTHER("其他");

    private final String description;

    PaymentMethod(String description) {
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
