package com.lawfirm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 案件统计DTO
 */
@Data
public class CaseStatisticsDTO {

    /**
     * 总案件数
     */
    private Long totalCases;

    /**
     * 进行中案件数
     */
    private Long activeCases;

    /**
     * 已结案数
     */
    private Long closedCases;

    /**
     * 本月新增案件数
     */
    private Long thisMonthCases;

    /**
     * 按类型分布
     */
    private Map<String, Long> casesByType;

    /**
     * 按状态分布
     */
    private Map<String, Long> casesByStatus;

    /**
     * 胜诉率（百分比）
     */
    private BigDecimal winRate;
}
