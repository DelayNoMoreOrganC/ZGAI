package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办DTO
 */
@Data
public class TodoDTO {

    private Long id;

    @NotBlank(message = "待办标题不能为空")
    private String title;

    private String description;

    private String status = "PENDING";

    private String priority = "NORMAL";

    private LocalDateTime dueDate;

    @NotNull(message = "负责人不能为空")
    private Long assigneeId;

    private String assigneeName;

    private Long caseId;

    private String caseName;

    private Boolean reminder = false;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 是否逾期
     */
    private Boolean overdue;

    /**
     * 剩余天数
     */
    private Long remainingDays;
}
