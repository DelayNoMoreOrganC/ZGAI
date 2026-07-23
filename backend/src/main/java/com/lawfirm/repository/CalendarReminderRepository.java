package com.lawfirm.repository;

import com.lawfirm.entity.CalendarReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CalendarReminderRepository extends JpaRepository<CalendarReminder, Long> {
    List<CalendarReminder> findByCalendarIdAndDeletedFalseOrderByOffsetMinutesDesc(Long calendarId);
    List<CalendarReminder> findByStatusAndDeletedFalse(String status);
}
