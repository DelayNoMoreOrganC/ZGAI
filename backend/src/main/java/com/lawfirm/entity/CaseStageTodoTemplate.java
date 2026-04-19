package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 案件阶段待办模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_stage_todo_template")
public class CaseStageTodoTemplate extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 流程模板ID
     */
    @NotNull(message = "流程模板ID不能为空")
    @Column(name = "flow_template_id", nullable = false)
    private Long flowTemplateId;

    /**
     * 阶段名称
     */
    @NotBlank(message = "阶段名称不能为空")
    @Column(name = "stage_name", nullable = false, length = 50)
    private String stageName;

    /**
     * 阶段顺序
     */
    @NotNull(message = "阶段顺序不能为空")
    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    /**
     * 待办事项标题
     */
    @NotBlank(message = "待办事项标题不能为空")
    @Column(nullable = false, length = 200)
    private String todoTitle;

    /**
     * 待办事项描述
     */
    private String todoDescription;

    /**
     * 优先级
     */
    @Column(length = 20)
    private String priority = "MEDIUM";

    /**
     * 截止天数（相对于阶段开始日期）
     */
    @Column(name = "due_days")
    private Integer dueDays;

    /**
     * 负责人类型（OWNER/CO_OWNER/ASSISTANT/SPECIFIC）
     */
    @Column(name = "assignee_type", length = 20)
    private String assigneeType = "OWNER";

    /**
     * 排序
     */
    @Column(nullable = false)
    private Integer sortOrder = 0;
}
