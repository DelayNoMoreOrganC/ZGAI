package com.lawfirm.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 律师业绩统计DTO
 */
@Data
public class LawyerPerformanceDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 律师姓名
     */
    private String lawyerName;

    /**
     * 负责案件总数
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
     * 总收费金额
     */
    private BigDecimal totalFees;

    /**
     * 本月收费
     */
    private BigDecimal thisMonthFees;

    /**
     * 结案率（百分比）
     */
    private BigDecimal closureRate;

    /**
     * 排名
     */
    private Integer rank;
}
