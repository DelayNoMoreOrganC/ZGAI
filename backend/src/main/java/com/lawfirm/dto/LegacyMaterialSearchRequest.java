package com.lawfirm.dto;

import lombok.Data;

/**
 * 旧系统资料检索请求。
 */
@Data
public class LegacyMaterialSearchRequest {

    private String keyword;

    private String caseName;

    private String caseNumber;

    private String clientName;

    private String caseReason;

    private String ownerName;

    private String departmentName;

    private Integer limit;
}
