package com.lawfirm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 类案检索请求DTO
 */
@Data
@Schema(description = "类案检索请求")
public class CaseSearchRequest {

    @Schema(description = "案由", required = true)
    @NotBlank(message = "案由不能为空")
    private String caseReason;

    @Schema(description = "案件类型", required = true)
    @NotBlank(message = "案件类型不能为空")
    private String caseType;

    @Schema(description = "争议金额")
    private BigDecimal amount;

    @Schema(description = "管辖法院")
    private String court;

    @Schema(description = "排除的案例ID（检索自己案例时使用）")
    private Long excludeCaseId;

    @Schema(description = "返回结果数量限制")
    private Integer limit = 20;
}