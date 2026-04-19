package com.lawfirm.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 更新案件请求DTO
 */
@Data
public class CaseUpdateRequest {

    /**
     * 案件名称
     */
    private String caseName;

    /**
     * 案件类型
     */
    private String caseType;

    /**
     * 案件程序
     */
    private String procedure;

    /**
     * 案由
     */
    private String caseReason;

    /**
     * 管辖法院
     */
    private String court;

    /**
     * 立案时间
     */
    private LocalDate filingDate;

    /**
     * 审限时间
     */
    private LocalDate deadlineDate;

    /**
     * 委托时间
     */
    private LocalDate commissionDate;

    /**
     * 案件标签
     */
    private String tags;

    /**
     * 案件简述
     */
    private String summary;

    /**
     * 案件等级
     */
    private String level;

    /**
     * 主办律师ID
     */
    private Long ownerId;

    /**
     * 协办律师ID列表
     */
    private List<Long> coOwnerIds;

    /**
     * 律师助理ID列表
     */
    private List<Long> assistantIds;

    /**
     * 标的额
     */
    private BigDecimal amount;

    /**
     * 代理费
     */
    private BigDecimal attorneyFee;

    /**
     * 收费方式
     */
    private String feeMethod;

    /**
     * 收费简介
     */
    private String feeDescription;

    /**
     * 收费备注
     */
    private String feeNotes;

    /**
     * 胜诉金额
     */
    private BigDecimal wonAmount;

    /**
     * 实际回款
     */
    private BigDecimal actualReceived;

    /**
     * 结案状态
     */
    private String closeStatus;

    /**
     * 结案日期
     */
    private LocalDate closeDate;

    /**
     * 归档日期
     */
    private LocalDate archiveDate;

    /**
     * 档案保管地
     */
    private String archiveLocation;

    /**
     * 当事人列表（全量更新）
     */
    @Valid
    private List<PartyDTO> parties;

    /**
     * 关联客户ID列表
     */
    private List<Long> clientIds;

    /**
     * 关联案件ID列表
     */
    private List<Long> relatedCaseIds;
}
