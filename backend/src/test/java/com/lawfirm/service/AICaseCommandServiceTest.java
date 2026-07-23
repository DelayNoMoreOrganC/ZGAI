package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AICaseCommandRequest;
import com.lawfirm.dto.AICaseCommandResponse;
import com.lawfirm.dto.CalendarDTO;
import com.lawfirm.entity.AICaseCommand;
import com.lawfirm.entity.Case;
import com.lawfirm.repository.AICaseCommandRepository;
import com.lawfirm.repository.CaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AICaseCommandServiceTest {
    private AICaseCommandRepository commandRepository;
    private CalendarService calendarService;
    private CalendarReminderService calendarReminderService;
    private CaseTimelineService timelineService;
    private CaseActivityService activityService;
    private CaseStageService stageService;
    private AICaseCommandService service;

    @BeforeEach
    void setUp() {
        commandRepository = mock(AICaseCommandRepository.class);
        CaseRepository caseRepository = mock(CaseRepository.class);
        CaseService caseService = mock(CaseService.class);
        calendarService = mock(CalendarService.class);
        calendarReminderService = mock(CalendarReminderService.class);
        TodoService todoService = mock(TodoService.class);
        timelineService = mock(CaseTimelineService.class);
        activityService = mock(CaseActivityService.class);
        stageService = mock(CaseStageService.class);

        Case caseEntity = new Case();
        caseEntity.setId(7L);
        caseEntity.setCaseName("测试案件");
        caseEntity.setStatus("ACTIVE");
        when(caseRepository.findById(7L)).thenReturn(Optional.of(caseEntity));
        when(commandRepository.findByUserIdAndIdempotencyKeyAndDeletedFalse(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        AtomicLong ids = new AtomicLong(1);
        when(commandRepository.save(any(AICaseCommand.class))).thenAnswer(invocation -> {
            AICaseCommand command = invocation.getArgument(0);
            if (command.getId() == null) command.setId(ids.getAndIncrement());
            return command;
        });
        CalendarDTO savedCalendar = new CalendarDTO();
        savedCalendar.setId(20L);
        when(calendarService.createCalendar(any(CalendarDTO.class), eq(3L))).thenReturn(savedCalendar);

        service = new AICaseCommandService(commandRepository, caseRepository, caseService,
                calendarService, calendarReminderService, todoService, timelineService,
                activityService, stageService, new ObjectMapper());
    }

    @Test
    void incompleteHearingInstructionOnlyRequestsClarification() {
        AICaseCommandResponse response = service.submit(request("本案8月10日9:30开庭", "missing-location"), 3L);

        assertEquals("NEEDS_CLARIFICATION", response.getStatus());
        assertEquals("请补充开庭或听证地点。", response.getClarification());
        verifyNoInteractions(calendarService, calendarReminderService, timelineService, activityService);
    }

    @Test
    void explicitHearingCreatesCalendarAndThreeReminderLevels() {
        AICaseCommandResponse response = service.submit(
                request("本案2099年8月10日9:30开庭，地点三号法庭", "complete-hearing"), 3L);

        assertEquals("AUTO_EXECUTED", response.getStatus());
        assertNotNull(response.getCommandId());
        verify(calendarService).createCalendar(argThat(calendar ->
                calendar.getCaseId().equals(7L)
                        && calendar.getStartTime().getHour() == 9
                        && calendar.getStartTime().getMinute() == 30
                        && "三号法庭".equals(calendar.getLocation())), eq(3L));
        verify(calendarReminderService).scheduleUpcomingHearingReminders(eq(20L), any());
        verify(timelineService).createSystemTimeline(eq(7L), eq("AI_CALENDAR_CREATED"),
                eq("已登记开庭：2099-08-10T09:30，地点：三号法庭"));
        verify(activityService).create(eq(7L), eq("HEARING"), anyString(), anyString(),
                any(), eq("AI_COMMAND"), anyLong(), eq(3L), isNull(), anyString());
    }

    @Test
    void relativeHearingTimeSupportsCommonChineseExpression() {
        AICaseCommandResponse response = service.submit(
                request("本案明天下午3点30分开庭，地点第二审判庭", "relative-hearing"), 3L);

        assertEquals("AUTO_EXECUTED", response.getStatus());
        verify(calendarService).createCalendar(argThat(calendar ->
                calendar.getStartTime().toLocalDate().equals(LocalDate.now().plusDays(1))
                        && calendar.getStartTime().getHour() == 15
                        && calendar.getStartTime().getMinute() == 30
                        && "第二审判庭".equals(calendar.getLocation())), eq(3L));
    }

    @Test
    void expiredExplicitHearingOnlyRequestsClarification() {
        AICaseCommandResponse response = service.submit(
                request("本案2020年8月10日9:30开庭，地点三号法庭", "expired-hearing"), 3L);

        assertEquals("NEEDS_CLARIFICATION", response.getStatus());
        assertEquals("开庭或听证时间已经过去，请补充未来的日期和时间。", response.getClarification());
        verifyNoInteractions(calendarService, calendarReminderService, timelineService, activityService);
    }

    @Test
    void stageChangeRejectsSkippingTheNextStageBeforeProposal() {
        when(stageService.getNextStageName(7L)).thenReturn(Optional.of("签约立案"));

        AICaseCommandResponse response = service.submit(
                request("本案进入诉前准备阶段", "skip-stage"), 3L);

        assertEquals("NEEDS_CLARIFICATION", response.getStatus());
        assertEquals("当前只能进入下一阶段「签约立案」，请确认后重新提交。", response.getClarification());
        verify(stageService, never()).changeStatus(anyLong(), anyString(), anyString(), anyLong());
    }

    @Test
    void validNextStageIsOnlyProposedAndNotExecuted() {
        when(stageService.getNextStageName(7L)).thenReturn(Optional.of("签约立案"));

        AICaseCommandResponse response = service.submit(
                request("本案进入签约立案阶段", "next-stage"), 3L);

        assertEquals("PROPOSED", response.getStatus());
        assertEquals("CHANGE_STAGE", response.getActions().get(0).getActionType());
        assertEquals("签约立案", response.getActions().get(0).getPayload().get("targetStage"));
        verify(stageService, never()).changeStatus(anyLong(), anyString(), anyString(), anyLong());
    }

    @Test
    void commandAuditStoresOnlyRedactedInstructionSummaryAndHash() {
        service.submit(request(
                "记录进展：已联系13800138000，固话020-12345678，证件440101199001011234",
                "privacy-audit"), 3L);

        org.mockito.ArgumentCaptor<AICaseCommand> captor =
                org.mockito.ArgumentCaptor.forClass(AICaseCommand.class);
        verify(commandRepository, atLeastOnce()).save(captor.capture());
        AICaseCommand command = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertFalse(command.getInstruction().contains("记录进展"));
        assertFalse(command.getInstruction().contains("13800138000"));
        assertFalse(command.getInstruction().contains("020-12345678"));
        assertFalse(command.getInstruction().contains("440101199001011234"));
        assertEquals(64, command.getInstructionHash().length());
        assertNotNull(command.getPrivacySanitizedAt());
    }

    private AICaseCommandRequest request(String instruction, String key) {
        AICaseCommandRequest request = new AICaseCommandRequest();
        request.setCaseId(7L);
        request.setInstruction(instruction);
        request.setIdempotencyKey(key);
        return request;
    }
}
