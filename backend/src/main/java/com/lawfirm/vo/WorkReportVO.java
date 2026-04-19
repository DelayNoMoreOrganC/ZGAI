package com.lawfirm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作汇报VO
 */
@Data
public class WorkReportVO {

    private Long id;

    private String title;

    private LocalDateTime reportDate;

    private String reportType;

    private String content;

    private String workSummary;

    private String nextPlan;

    private String problems;

    private String suggestions;

    private Long reporterId;

    private String reporterName;

    private String department;

    private String status;

    private Long reviewerId;

    private String reviewerName;

    private String reviewComment;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
