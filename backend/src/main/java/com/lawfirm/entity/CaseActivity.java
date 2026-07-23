package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_activity", indexes = {
        @Index(name = "idx_case_activity_case_time", columnList = "case_id,occurred_at"),
        @Index(name = "idx_case_activity_source", columnList = "source_type,source_id")
})
public class CaseActivity extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotBlank
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    private String content;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @NotBlank
    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @NotNull
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "procedure_stage", length = 80)
    private String procedureStage;

    @Lob
    @Column(name = "metadata_json")
    private String metadataJson;
}
