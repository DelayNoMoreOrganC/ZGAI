package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 案件程序实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_procedure")
public class CaseProcedure extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotBlank(message = "程序名称不能为空")
    @Column(name = "procedure_name", nullable = false, length = 50)
    private String procedureName;

    @Column(name = "procedure_number", length = 50)
    private String procedureNumber;

    @Column(name = "procedure_type", length = 20)
    private String procedureType;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "judgment_date")
    private LocalDate judgmentDate;

    @Column(length = 50)
    private String result;

    @Column(length = 100)
    private String court;

    @Column(length = 50)
    private String region;

    @Column(length = 50)
    private String judge;

    private String notes;

    @Column
    private String attachments;
}
