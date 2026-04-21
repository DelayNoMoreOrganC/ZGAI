package com.lawfirm.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class CaseCreateRequest {
    // 基本信息
    @NotBlank(message = "案件编号不能为空")
    private String caseNumber;

    @NotBlank(message = "案件名称不能为空")
    private String caseName;

    @NotBlank(message = "案件类型不能为空")
    private String caseType;

    @NotNull(message = "案件程序不能为空")
    private String procedure;

    @NotNull(message = "案件等级不能为空")
    private String level;

    @NotNull(message = "主办律师不能为空")
    private Long ownerId;

    // 团队成员
    private List<Long> coOwnerIds;
    private List<Long> assistantIds;

    // 案件详情
    private String caseReason;
    private String court;
    private LocalDate filingDate;
    private LocalDate deadlineDate;
    private LocalDate commissionDate;

    // 标签和摘要
    private String tags;
    private String summary;

    // 案件状态
    private String status;

    // 当前阶段
    private String currentStage;

    // 金额相关
    private java.math.BigDecimal amount;
    private java.math.BigDecimal attorneyFee;
    private String feeMethod;

    // 当事人信息
    private List<PartyDTO> parties;

    // 应收款信息
    private List<ReceivableRequest> receivables;

    @Data
    public static class ReceivableRequest {
        private String name;
        private java.math.BigDecimal amount;
        private String dueDate;
        private String notes;
    }
}
