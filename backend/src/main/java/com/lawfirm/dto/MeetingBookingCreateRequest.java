package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室预约创建请求
 */
@Data
public class MeetingBookingCreateRequest {

    @NotNull(message = "会议室ID不能为空")
    private Long roomId;

    @NotBlank(message = "会议主题不能为空")
    private String meetingTitle;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private String attendees;

    private Long caseId;

    private String notes;
}
