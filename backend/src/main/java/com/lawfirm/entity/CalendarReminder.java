package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "calendar_reminder", uniqueConstraints = {
        @UniqueConstraint(name = "uk_calendar_reminder_offset", columnNames = {"calendar_id", "offset_minutes"})
})
public class CalendarReminder extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "calendar_id", nullable = false)
    private Long calendarId;

    @NotNull
    @Column(name = "offset_minutes", nullable = false)
    private Integer offsetMinutes;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";
}
