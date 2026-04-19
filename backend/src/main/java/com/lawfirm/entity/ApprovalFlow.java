package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批流程记录实体
 */
@Data
@Entity
@Table(name = "approval_flow")
public class ApprovalFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "审批单ID不能为空")
    @Column(name = "approval_id", nullable = false)
    private Long approvalId;

    @NotNull(message = "审批人不能为空")
    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    @NotBlank(message = "审批动作不能为空")
    @Column(nullable = false, length = 20)
    private String action;

    private String comments;

    @NotNull(message = "审批时间不能为空")
    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
