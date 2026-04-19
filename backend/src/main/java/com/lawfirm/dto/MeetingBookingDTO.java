package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室预约DTO
 */
@Data
public class MeetingBookingDTO {
    private Long id;
    private Long roomId;
    private String roomName;
    private String meetingTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long bookerId;
    private String bookerName;
    private String attendees;
    private Long caseId;
    private String caseName;
    private String status;
    private String statusDesc;
    private String notes;
}
