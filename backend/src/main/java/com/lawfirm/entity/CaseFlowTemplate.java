package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 案件流程模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_flow_template")
public class CaseFlowTemplate extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Column(nullable = false, length = 100)
    private String templateName;

    /**
     * 案件类型
     */
    @NotBlank(message = "案件类型不能为空")
    @Column(nullable = false, length = 20)
    private String caseType;

    /**
     * 是否为系统预置模板
     */
    @Column(nullable = false)
    private Boolean isSystem = false;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 排序
     */
    @Column(nullable = false)
    private Integer sortOrder = 0;

    /**
     * 描述
     */
    private String description;
}
