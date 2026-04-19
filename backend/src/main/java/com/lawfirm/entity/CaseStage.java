package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 案件阶段实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_stage")
public class CaseStage extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotBlank(message = "阶段名称不能为空")
    @Column(name = "stage_name", nullable = false, length = 50)
    private String stageName;

    @NotNull(message = "阶段顺序不能为空")
    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
