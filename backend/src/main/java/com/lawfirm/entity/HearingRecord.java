package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 庭审记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "hearing_record")
public class HearingRecord extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotNull(message = "庭审日期不能为空")
    @Column(name = "hearing_date", nullable = false)
    private LocalDate hearingDate;

    @NotNull(message = "庭审时间不能为空")
    @Column(name = "hearing_time", nullable = false)
    private LocalDateTime hearingTime;

    @NotBlank(message = "法庭地点不能为空")
    @Column(name = "court_location", nullable = false, length = 200)
    private String courtLocation;

    @NotBlank(message = "庭审类型不能为空")
    @Column(name = "hearing_type", nullable = false, length = 50)
    private String hearingType; // FIRST_INSTANCE, SECOND_INSTANCE, PRETRIAL, EVIDENCE_EXCHANGE

    @Column(name = "judge", length = 100)
    private String judge;

    @Column(name = "clerk", length = 100)
    private String clerk;

    @Column(name = "opposing_lawyers", length = 500)
    private String opposingLawyers;

    @Lob
    @Column(name = "hearing_summary")
    private String hearingSummary;

    @Lob
    @Column(name = "key_arguments")
    private String keyArguments;

    @Lob
    @Column(name = "evidence_submitted")
    private String evidenceSubmitted;

    @Lob
    @Column(name = "court_focus")
    private String courtFocus;

    @Column(name = "next_hearing_date")
    private LocalDate nextHearingDate;

    @Lob
    @Column(name = "attachments")
    private String attachments;

    @Lob
    @Column(name = "remarks")
    private String remarks;

    @NotNull(message = "创建人不能为空")
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
