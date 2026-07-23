package com.lawfirm.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 利冲命中项。仅返回核查所需摘要，不暴露无权客户或案件详情。
 */
@Data
@Builder
public class ConflictCheckHitDTO {

    private String sourceType;
    private String subjectName;
    private String subjectRole;
    private String matchType;
    private String riskLevel;
    private Integer relatedCaseCount;
    private String reason;
}
