package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户DTO
 */
@Data
public class ClientDTO {

    private Long id;

    @NotBlank(message = "客户类型不能为空")
    private String clientType;

    @NotBlank(message = "客户姓名/名称不能为空")
    private String clientName;

    private String clientRelationship;

    private String clientRole;

    private String gender;

    private String ethnicity;

    private String idCard;

    private String creditCode;

    private String phone;

    private String email;

    private String address;

    private String contactPerson;

    private String wechat;

    private String legalRepresentative;

    private String legalRepresentativeIdCard;

    private String invoiceTitle;

    private String invoiceTaxNo;

    private String invoiceAddressPhone;

    private String invoiceBankAccount;

    private String opposingLawyer;

    private String industry;

    private String status = "ACTIVE";

    private String source;

    private String notes;

    private Long departmentId;

    private String departmentName;

    private Long ownerId;

    private String ownerName;

    private String sourceUserIds;

    private String sourceUserNames;

    private String clientOwnerIds;

    private String clientOwnerNames;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 关联案件数
     */
    private Integer caseCount;

    /**
     * 沟通记录数
     */
    private Integer communicationCount;

    /**
     * 最近沟通时间
     */
    private LocalDateTime lastCommunicationDate;

    /**
     * 是否存在利益冲突
     */
    private Boolean hasConflict;

    private String conflictLevel;

    private String conflictDescription;

    private List<String> similarClientNames;

    /**
     * 冲突案件列表
     */
    private List<Long> conflictCaseIds;
}
