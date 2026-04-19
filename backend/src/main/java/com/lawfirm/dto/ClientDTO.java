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

    private String gender;

    private String idCard;

    private String creditCode;

    private String phone;

    private String email;

    private String address;

    private String legalRepresentative;

    private String industry;

    private String status = "ACTIVE";

    private String source;

    private String notes;

    private Long ownerId;

    private String ownerName;

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

    /**
     * 冲突案件列表
     */
    private List<Long> conflictCaseIds;
}
