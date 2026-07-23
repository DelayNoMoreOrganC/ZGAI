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
    RESPONDENT("被申请人"),
    APPELLANT("上诉人"),
    APPELLEE("被上诉人"),
    CLIENT("委托人"),
    COUNTERCLAIMANT("反请求申请人"),
    COUNTER_RESPONDENT("反请求被申请人"),
    SUSPECT("犯罪嫌疑人"),
    DEFENDANT_CRIMINAL("被告人"),
    VICTIM("被害人"),
    FAMILY_MEMBER("近亲属"),
    ADMINISTRATIVE_COUNTERPART("行政相对人"),
    ADMINISTRATIVE_AUTHORITY("行政机关"),
    TARGET_COMPANY("目标公司"),
    COUNTERPARTY("交易/事务相对方"),
    INVESTOR("投资方"),
    FINANCIER("融资方"),
    CREDITOR("债权人"),
    DEBTOR("债务人"),
    OTHER_PARTICIPANT("其他参与方"),
    CONSULTANT_UNIT("顾问单位"),
    SERVICE_RECIPIENT("服务对象"),
    RELATED_COMPANY("关联公司");

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
