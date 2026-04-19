package com.lawfirm.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 案件详情VO
 */
@Data
public class CaseDetailVO {

    /**
     * 基本信息
     */
    private Long id;
    private String caseNumber;
    private String caseName;
    private String caseType;
    private String caseTypeDesc;
    private String procedure;
    private String caseReason;
    private String court;
    private LocalDate filingDate;
    private LocalDate deadlineDate;
    private LocalDate commissionDate;
    private LocalDate closeDate;
    private LocalDate archiveDate;
    private String archiveLocation;
    private String summary;
    private String tags;
    private String level;
    private String levelDesc;
    private String status;
    private String statusDesc;
    private String currentStage;
    private BigDecimal amount;
    private BigDecimal attorneyFee;
    private String feeMethod;
    private String feeDescription;
    private String feeNotes;
    private BigDecimal wonAmount;
    private BigDecimal actualReceived;
    private String closeStatus;
    private String closeStatusDesc;

    /**
     * 团队信息
     */
    private Long ownerId;
    private String ownerName;
    private List<MemberVO> coOwners;
    private List<MemberVO> assistants;

    /**
     * 当事人列表
     */
    private List<PartyVO> parties;

    /**
     * 案件程序列表
     */
    private List<CaseProcedureVO> procedures;

    /**
     * 关联客户ID列表
     */
    private List<Long> clientIds;

    /**
     * 关联案件列表
     */
    private List<RelatedCaseVO> relatedCases;

    /**
     * 阶段进度
     */
    private List<StageProgressVO> stageProgress;

    /**
     * 权限信息
     */
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canArchive;
    private Boolean canChangeStatus;

    /**
     * 成员VO
     */
    @Data
    public static class MemberVO {
        private Long id;
        private String name;
        private String role;
    }

    /**
     * 关联案件VO
     */
    @Data
    public static class RelatedCaseVO {
        private Long id;
        private String caseName;
        private String caseNumber;
        private String caseType;
        private String status;
    }

    /**
     * 阶段进度VO
     */
    @Data
    public static class StageProgressVO {
        private String stageName;
        private Integer stageOrder;
        private String status;
        private String statusDesc;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
