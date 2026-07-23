package com.lawfirm.service;

import com.lawfirm.entity.CalendarReminder;
import com.lawfirm.repository.CalendarReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CalendarReminderService {

    private static final int[] HEARING_REMINDER_OFFSETS = {10080, 1440, 120};

    private final CalendarReminderRepository reminderRepository;

    @Transactional
    public int scheduleUpcomingHearingReminders(Long calendarId, LocalDateTime hearingTime) {
        if (calendarId == null || hearingTime == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        for (int offset : HEARING_REMINDER_OFFSETS) {
            if (!hearingTime.minusMinutes(offset).isAfter(now)) continue;
            CalendarReminder reminder = new CalendarReminder();
            reminder.setCalendarId(calendarId);
            reminder.setOffsetMinutes(offset);
            reminderRepository.save(reminder);
            created++;
        }
        return created;
    }
}
