package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 案件记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_record")
public class CaseRecord extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotBlank(message = "记录标题不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "记录内容不能为空")
    @Column(nullable = false)
    private String content;

    @Column(length = 50)
    private String stage;

    @Column(name = "work_hours", precision = 4, scale = 1)
    private BigDecimal workHours;

    @NotNull(message = "记录日期不能为空")
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @NotNull(message = "创建人不能为空")
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column
    private String attachments;
}
