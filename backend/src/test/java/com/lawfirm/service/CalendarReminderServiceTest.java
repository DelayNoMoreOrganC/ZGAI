package com.lawfirm.service;

import com.lawfirm.entity.CalendarReminder;
import com.lawfirm.repository.CalendarReminderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CalendarReminderServiceTest {

    @Test
    void createsAllThreeStandardRemindersForDistantHearing() {
        CalendarReminderRepository repository = mock(CalendarReminderRepository.class);
        CalendarReminderService service = new CalendarReminderService(repository);

        int created = service.scheduleUpcomingHearingReminders(9L, LocalDateTime.of(2099, 8, 10, 9, 30));

        assertEquals(3, created);
        ArgumentCaptor<CalendarReminder> captor = ArgumentCaptor.forClass(CalendarReminder.class);
        verify(repository, times(3)).save(captor.capture());
        assertEquals(List.of(10080, 1440, 120), captor.getAllValues().stream()
                .map(CalendarReminder::getOffsetMinutes).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void skipsReminderWindowsThatHaveAlreadyPassed() {
        CalendarReminderRepository repository = mock(CalendarReminderRepository.class);
        CalendarReminderService service = new CalendarReminderService(repository);

        int created = service.scheduleUpcomingHearingReminders(9L, LocalDateTime.now().plusHours(3));

        assertEquals(1, created);
        verify(repository, times(1)).save(any(CalendarReminder.class));
    }
}
