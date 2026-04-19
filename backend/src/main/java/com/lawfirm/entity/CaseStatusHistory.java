package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 案件状态历史记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_status_history")
public class CaseStatusHistory extends BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 案件ID
     */
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    /**
     * 原状态
     */
    @Column(name = "old_status", length = 50)
    private String oldStatus;

    /**
     * 新状态
     */
    @Column(name = "new_status", nullable = false, length = 50)
    private String newStatus;

    /**
     * 状态变更原因
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * 操作人ID
     */
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @Column(name = "operator_name", length = 50)
    private String operatorName;

    /**
     * 状态变更时间
     */
    @Column(name = "change_time", nullable = false)
    private LocalDateTime changeTime;
}
