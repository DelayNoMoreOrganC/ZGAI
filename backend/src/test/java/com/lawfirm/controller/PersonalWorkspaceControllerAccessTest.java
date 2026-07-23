package com.lawfirm.controller;

import com.lawfirm.dto.CalendarDTO;
import com.lawfirm.dto.TodoDTO;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.CalendarService;
import com.lawfirm.service.CaseService;
import com.lawfirm.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalWorkspaceControllerAccessTest {

    private TodoService todoService;
    private CalendarService calendarService;
    private CaseService caseService;
    private SecurityUtils securityUtils;
    private TodoController todoController;
    private CalendarController calendarController;

    @BeforeEach
    void setUp() {
        todoService = mock(TodoService.class);
        calendarService = mock(CalendarService.class);
        securityUtils = mock(SecurityUtils.class);
        caseService = mock(CaseService.class);
        todoController = new TodoController(todoService, caseService, securityUtils);
        calendarController = new CalendarController(calendarService, caseService, securityUtils);
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        when(securityUtils.isAdmin()).thenReturn(false);
    }

    @Test
    void ordinaryUserCannotReadAnotherUsersTodoLists() {
        assertThrows(AccessDeniedException.class, () -> todoController.getTodosByAssignee(8L));
        assertThrows(AccessDeniedException.class,
                () -> todoController.getTodosByFilter(8L, "pending", "urgency"));
    }

    @Test
    void todoSearchDefaultsToCurrentUserAndNormalizesStatus() {
        when(todoService.getPendingTodos(7L)).thenReturn(java.util.Collections.emptyList());

        todoController.getTodosByFilter(null, "PENDING", "urgency");

        verify(todoService).getPendingTodos(7L);
        verify(todoService).sortByUrgency(java.util.Collections.emptyList());
    }

    @Test
    void ordinaryUserCannotModifyAnotherUsersTodo() {
        TodoDTO todo = new TodoDTO();
        todo.setId(10L);
        todo.setAssigneeId(8L);
        when(todoService.getTodoById(10L)).thenReturn(todo);

        assertThrows(AccessDeniedException.class, () -> todoController.deleteTodo(10L));
    }

    @Test
    void ordinaryUserCannotReadAnotherUsersCalendar() {
        assertThrows(AccessDeniedException.class, () -> calendarController.getCalendarsByUser(8L));
    }

    @Test
    void calendarParticipantCanReadButCannotModifyEvent() {
        CalendarDTO calendar = new CalendarDTO();
        calendar.setId(20L);
        calendar.setCreatedBy(8L);
        calendar.setParticipantIds(java.util.List.of("7"));
        when(calendarService.getCalendarById(20L)).thenReturn(calendar);

        assertDoesNotThrow(() -> calendarController.getCalendar(20L));
        assertThrows(AccessDeniedException.class, () -> calendarController.deleteCalendar(20L));
    }

    @Test
    void caseLinkedCalendarRequiresCaseEditPermission() {
        CalendarDTO calendar = new CalendarDTO();
        calendar.setCaseId(99L);

        calendarController.createCalendar(calendar);

        verify(caseService).assertCaseEditable(99L, 7L);
    }

    @Test
    void caseLinkedTodoRequiresCaseEditPermission() {
        TodoDTO todo = new TodoDTO();
        todo.setCaseId(99L);
        todo.setAssigneeId(7L);

        todoController.createTodo(todo);

        verify(caseService).assertCaseEditable(99L, 7L);
    }
}
