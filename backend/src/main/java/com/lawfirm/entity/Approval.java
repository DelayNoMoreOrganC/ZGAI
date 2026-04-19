package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "approval")
public class Approval extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "审批类型不能为空")
    @Column(name = "approval_type", nullable = false, length = 50)
    private String approvalType;

    @NotBlank(message = "审批标题不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "审批内容不能为空")
    @Column(nullable = false)
    private String content;

    @Column(name = "case_id")
    private Long caseId;

    @NotNull(message = "申请人不能为空")
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "current_approver_id")
    private Long currentApproverId;

    @Column(length = 20)
    private String status = "PENDING";

    @NotNull(message = "申请时间不能为空")
    @Column(name = "apply_time", nullable = false)
    private LocalDateTime applyTime;

    @Column(name = "approved_time")
    private LocalDateTime approvedTime;

    @Column(name = "approval_notes")
    private String approvalNotes;

    @Column
    private String attachments;
}
