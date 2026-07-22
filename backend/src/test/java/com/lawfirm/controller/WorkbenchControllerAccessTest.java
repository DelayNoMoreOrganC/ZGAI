package com.lawfirm.controller;

import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.CalendarService;
import com.lawfirm.service.StatisticsService;
import com.lawfirm.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkbenchControllerAccessTest {

    private TodoService todoService;
    private CalendarService calendarService;
    private SecurityUtils securityUtils;
    private WorkbenchController controller;

    @BeforeEach
    void setUp() {
        todoService = mock(TodoService.class);
        calendarService = mock(CalendarService.class);
        securityUtils = mock(SecurityUtils.class);
        controller = new WorkbenchController(
                mock(StatisticsService.class), todoService, calendarService, securityUtils);
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        when(todoService.getTodosByAssignee(7L)).thenReturn(Collections.emptyList());
        when(calendarService.getCalendarsByUser(7L)).thenReturn(Collections.emptyList());
    }

    @Test
    void ordinaryUserCanReadOwnPersonalStats() {
        assertDoesNotThrow(() -> controller.getTodoStats(7L));
        assertDoesNotThrow(() -> controller.getCalendarStats(7L, null, null));
        verify(todoService).getTodosByAssignee(7L);
        verify(calendarService).getCalendarsByUser(7L);
    }

    @Test
    void ordinaryUserCannotReadAnotherUsersPersonalStats() {
        when(securityUtils.isAdmin()).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> controller.getTodoStats(8L));
        assertThrows(AccessDeniedException.class, () -> controller.getCalendarStats(8L, null, null));
    }

    @Test
    void directorOrAdministratorCanReadAnotherUsersPersonalStats() {
        when(securityUtils.isAdmin()).thenReturn(true);
        when(todoService.getTodosByAssignee(8L)).thenReturn(Collections.emptyList());
        when(calendarService.getCalendarsByUser(8L)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> controller.getTodoStats(8L));
        assertDoesNotThrow(() -> controller.getCalendarStats(8L, null, null));
        verify(todoService).getTodosByAssignee(8L);
        verify(calendarService).getCalendarsByUser(8L);
    }
}
