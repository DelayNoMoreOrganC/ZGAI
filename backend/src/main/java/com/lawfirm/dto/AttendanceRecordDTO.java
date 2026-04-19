package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录DTO
 */
@Data
public class AttendanceRecordDTO {

    private Long id;

    /**
     * 考勤类型：LEAVE(请假)/BUSINESS_TRIP(出差)/OVERTIME(加班)
     */
    @NotBlank(message = "考勤类型不能为空")
    private String attendanceType;

    /**
     * 子类型：年假/病假/事假/出差/加班等
     */
    private String subType;

    /**
     * 申请人ID
     */
    @NotNull(message = "申请人不能为空")
    private Long userId;

    private String userName;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 时长（天或小时）
     */
    private BigDecimal duration;

    /**
     * 时长单位：DAY(天)/HOUR(小时)
     */
    private String durationUnit;

    /**
     * 事由
     */
    private String reason;

    /**
     * 审批状态：PENDING(待审批)/APPROVED(已通过)/REJECTED(已拒绝)
     */
    private String approvalStatus;

    /**
     * 审批人ID
     */
    private Long approverId;

    private String approverName;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 附件
     */
    private String attachments;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
