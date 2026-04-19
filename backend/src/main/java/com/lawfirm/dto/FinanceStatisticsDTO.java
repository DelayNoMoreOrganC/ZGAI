package com.lawfirm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 财务统计DTO
 */
@Data
public class FinanceStatisticsDTO {

    /**
     * 总收入
     */
    private BigDecimal totalRevenue;

    /**
     * 本月收入
     */
    private BigDecimal thisMonthRevenue;

    /**
     * 待收款金额
     */
    private BigDecimal pendingAmount;

    /**
     * 收款率（百分比）
     */
    private BigDecimal collectionRate;

    /**
     * 按月份收入趋势
     */
    private Map<String, BigDecimal> revenueByMonth;

    /**
     * 按类型分布
     */
    private Map<String, BigDecimal> revenueByType;
}
