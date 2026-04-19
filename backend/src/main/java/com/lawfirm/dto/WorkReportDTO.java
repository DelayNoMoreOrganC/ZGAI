package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 工作汇报DTO
 */
@Data
public class WorkReportDTO {

    @NotBlank(message = "汇报标题不能为空")
    private String title;

    @NotNull(message = "汇报日期不能为空")
    private LocalDateTime reportDate;

    @NotBlank(message = "汇报类型不能为空")
    private String reportType; // DAILY, WEEKLY, MONTHLY, PROJECT

    private String content;

    private String workSummary;

    private String nextPlan;

    private String problems;

    private String suggestions;

    private String department;

    private String status; // DRAFT, SUBMITTED, REVIEWED
}
