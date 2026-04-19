package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会议室预约实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "meeting_booking")
public class MeetingBooking extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "会议室ID不能为空")
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @NotBlank(message = "会议主题不能为空")
    @Column(name = "meeting_title", nullable = false, length = 200)
    private String meetingTitle;

    @NotNull(message = "开始时间不能为空")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotNull(message = "预约人不能为空")
    @Column(name = "booker_id", nullable = false)
    private Long bookerId;

    private String attendees;

    @Column(name = "case_id")
    private Long caseId;

    @Column(length = 20)
    private String status = "CONFIRMED";

    @Column
    private String notes;
}
