package com.lawfirm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 类案检索结果VO
 */
@Data
@Schema(description = "类案检索结果")
public class CaseSearchResultVO {

    @Schema(description = "案例ID")
    private Long caseId;

    @Schema(description = "案件名称")
    private String caseName;

    @Schema(description = "案号")
    private String caseNumber;

    @Schema(description = "案由")
    private String caseReason;

    @Schema(description = "案件类型")
    private String caseType;

    @Schema(description = "管辖法院")
    private String court;

    @Schema(description = "争议金额")
    private BigDecimal amount;

    @Schema(description = "案件摘要")
    private String summary;

    @Schema(description = "相似度分数（0.0-1.0）")
    private Double similarity;

    @Schema(description = "相似度百分比（显示用）")
    private String similarityPercent;
}