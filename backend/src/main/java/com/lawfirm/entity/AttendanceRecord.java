package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attendance_record")
public class AttendanceRecord extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 考勤类型：LEAVE(请假)/BUSINESS_TRIP(出差)/OVERTIME(加班)
     */
    @NotBlank(message = "考勤类型不能为空")
    @Column(name = "attendance_type", nullable = false, length = 20)
    private String attendanceType;

    /**
     * 子类型：年假/病假/事假/出差/加班等
     */
    @Column(name = "sub_type", length = 50)
    private String subType;

    /**
     * 申请人ID
     */
    @NotNull(message = "申请人不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * 时长（天或小时）
     */
    @Column(name = "duration", precision = 5, scale = 1)
    private BigDecimal duration;

    /**
     * 时长单位：DAY(天)/HOUR(小时)
     */
    @Column(name = "duration_unit", length = 10)
    private String durationUnit;

    /**
     * 事由
     */
    @Lob
    @Column(name = "reason")
    private String reason;

    /**
     * 审批状态：PENDING(待审批)/APPROVED(已通过)/REJECTED(已拒绝)
     */
    @Column(name = "approval_status", length = 20)
    private String approvalStatus = "PENDING";

    /**
     * 审批人ID
     */
    @Column(name = "approver_id")
    private Long approverId;

    /**
     * 审批时间
     */
    @Column(name = "approval_time")
    private LocalDateTime approvalTime;

    /**
     * 审批意见
     */
    @Lob
    @Column(name = "approval_comment")
    private String approvalComment;

    /**
     * 附件（如病假证明、出差文件等）
     */
    @Column(name = "attachments", length = 500)
    private String attachments;
}
