package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 案件动态实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_timeline")
public class CaseTimeline extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotBlank(message = "操作类型不能为空")
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @NotBlank(message = "操作内容不能为空")
    @Column(name = "action_content", nullable = false)
    private String actionContent;

    @NotNull(message = "操作人不能为空")
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "is_comment")
    private Boolean isComment = false;

    @Column(name = "parent_id")
    private Long parentId;

    @Column
    private String mentions;
}
