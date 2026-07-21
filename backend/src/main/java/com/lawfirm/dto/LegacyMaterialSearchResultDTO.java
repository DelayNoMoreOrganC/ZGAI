package com.lawfirm.dto;

import lombok.Data;

/**
 * 旧系统资料检索结果。
 */
@Data
public class LegacyMaterialSearchResultDTO {

    private String sourceType;

    private Long relatedId;

    private String title;

    private String caseNumber;

    private String clientName;

    private String caseReason;

    private String ownerName;

    private String departmentName;

    private String materialPath;

    private String matchReason;

    private Double score;
}
