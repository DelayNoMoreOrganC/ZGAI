package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 庭审记录DTO
 */
@Data
public class HearingRecordDTO {

    private Long id;
    private Long caseId;
    private String caseName;

    @NotNull(message = "庭审日期不能为空")
    private LocalDate hearingDate;

    @NotNull(message = "庭审时间不能为空")
    private LocalDateTime hearingTime;

    @NotBlank(message = "法庭地点不能为空")
    private String courtLocation;

    @NotBlank(message = "庭审类型不能为空")
    private String hearingType;

    private String judge;
    private String clerk;
    private String opposingLawyers;
    private String hearingSummary;
    private String keyArguments;
    private String evidenceSubmitted;
    private String courtFocus;
    private LocalDate nextHearingDate;
    private String attachments;
    private String remarks;
    private Long createdBy;
    private String createdByName;
}
