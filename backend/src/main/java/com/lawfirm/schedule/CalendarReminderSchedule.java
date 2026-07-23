package com.lawfirm.schedule;

import com.lawfirm.entity.Calendar;
import com.lawfirm.entity.CalendarReminder;
import com.lawfirm.repository.CalendarReminderRepository;
import com.lawfirm.repository.CalendarRepository;
import com.lawfirm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendarReminderSchedule {
    private final CalendarReminderRepository reminderRepository;
    private final CalendarRepository calendarRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (CalendarReminder reminder : reminderRepository.findByStatusAndDeletedFalse("PENDING")) {
            Calendar calendar = calendarRepository.findById(reminder.getCalendarId()).orElse(null);
            if (calendar == null || Boolean.TRUE.equals(calendar.getDeleted())) {
                reminder.setStatus("CANCELLED");
                reminderRepository.save(reminder);
                continue;
            }
            LocalDateTime remindAt = calendar.getStartTime().minusMinutes(reminder.getOffsetMinutes());
            if (remindAt.isAfter(now)) continue;
            notificationService.sendNotification(calendar.getCreatedBy(), "案件日程提醒",
                    calendar.getTitle() + "将于" + calendar.getStartTime() + "开始"
                            + (calendar.getLocation() == null ? "" : "，地点：" + calendar.getLocation()),
                    "CALENDAR_REMINDER", calendar.getId(), "Calendar");
            reminder.setStatus("SENT");
            reminderRepository.save(reminder);
        }
    }
}
