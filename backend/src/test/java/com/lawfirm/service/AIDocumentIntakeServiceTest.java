package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIDocumentIntakeConfirmRequest;
import com.lawfirm.dto.AIDocumentIntakeDTO;
import com.lawfirm.dto.CalendarDTO;
import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.dto.TodoDTO;
import com.lawfirm.entity.AIDocumentIntake;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.Party;
import com.lawfirm.exception.DocumentIntakeExpiredException;
import com.lawfirm.repository.AIDocumentIntakeRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.PartyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AIDocumentIntakeServiceTest {

    private AIDocumentIntakeRepository intakeRepository;
    private CaseRepository caseRepository;
    private PartyRepository partyRepository;
    private CaseService caseService;
    private CaseDocumentService caseDocumentService;
    private CalendarService calendarService;
    private CalendarReminderService reminderService;
    private TodoService todoService;
    private CaseActivityService activityService;
    private CaseTimelineService timelineService;
    private LocalDocumentTextService textService;
    private AIConfigService aiConfigService;
    private AIDocumentIntakeService service;

    @BeforeEach
    void setUp() {
        intakeRepository = mock(AIDocumentIntakeRepository.class);
        caseRepository = mock(CaseRepository.class);
        partyRepository = mock(PartyRepository.class);
        caseService = mock(CaseService.class);
        caseDocumentService = mock(CaseDocumentService.class);
        calendarService = mock(CalendarService.class);
        reminderService = mock(CalendarReminderService.class);
        todoService = mock(TodoService.class);
        activityService = mock(CaseActivityService.class);
        timelineService = mock(CaseTimelineService.class);
        textService = mock(LocalDocumentTextService.class);
        aiConfigService = mock(AIConfigService.class);

        service = new AIDocumentIntakeService(
                intakeRepository,
                caseRepository,
                partyRepository,
                caseService,
                caseDocumentService,
                calendarService,
                reminderService,
                todoService,
                activityService,
                timelineService,
                textService,
                aiConfigService,
                mock(OpenAICompatibleClient.class),
                new ObjectMapper());
    }

    @Test
    void localRulesExtractDigitalCourtCodeDeadlineAndAccessibleCandidate(@TempDir Path tempDir) throws Exception {
        ReflectionTestUtils.setField(service, "tempDir", tempDir.toString());
        String text = "佛 山 市 南 海 区 人 民 法 院 民 事 传 票\n"
                + "案 号 ：（2026）粤 0605 民 初 12345 号\n"
                + "甲 公 司 与 乙 公 司 买 卖 合 同 纠 纷\n"
                + "开 庭 时 间 ：2026 年 8 月 10 日 9 时 30 分\n"
                + "举 证 期 限 截 至 2026 年 8 月 5 日。";
        when(textService.extract(any(Path.class), eq("民事传票.png"), eq("image/png"))).thenReturn(text);
        when(aiConfigService.getUsableLocalDocumentConfigOrNull()).thenReturn(null);
        when(intakeRepository.save(any(AIDocumentIntake.class))).thenAnswer(call -> {
            AIDocumentIntake value = call.getArgument(0);
            if (value.getId() == null) value.setId(10L);
            return value;
        });
        Case target = new Case();
        target.setId(7L);
        target.setCaseName("甲公司与乙公司买卖合同纠纷");
        target.setCaseNumber("[2026]粤至高民字第001号");
        target.setCourtCaseNumber("(2026)粤0605民初12345号");
        target.setCourt("佛山市南海区人民法院");
        target.setDeleted(false);
        when(caseRepository.findByDeletedFalse()).thenReturn(java.util.List.of(target));
        when(caseService.canAccessCase(7L, 3L)).thenReturn(true);
        Party plaintiff = new Party();
        plaintiff.setName("甲公司");
        Party defendant = new Party();
        defendant.setName("乙公司");
        when(partyRepository.findByCaseIdAndDeletedFalse(7L))
                .thenReturn(java.util.List.of(plaintiff, defendant));

        AIDocumentIntakeDTO result = service.create(new MockMultipartFile(
                "file", "民事传票.png", "image/png", new byte[]{1, 2, 3}), 3L);

        assertEquals("ANALYZED", result.getStatus());
        assertEquals("（2026）粤0605民初12345号", result.getAnalysis().get("courtCaseNumber"));
        assertEquals("2026-08-10T09:30", result.getAnalysis().get("hearingDate"));
        assertEquals("2026-08-05", result.getAnalysis().get("deadline"));
        assertEquals(1, result.getCandidates().size());
        assertEquals(7L, result.getCandidates().get(0).getCaseId());
        assertEquals(100, result.getCandidates().get(0).getScore());
        verify(aiConfigService).getUsableLocalDocumentConfigOrNull();
    }

    @Test
    void anotherUserCannotReadStagedDocument(@TempDir Path tempDir) throws Exception {
        Path staged = tempDir.resolve("private.pdf");
        Files.write(staged, new byte[]{1});
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(analyzedIntake(staged)));

        assertThrows(AccessDeniedException.class, () -> service.get(10L, 4L));
    }

    @Test
    void inaccessibleCaseIsRejectedBeforeDocumentUpload(@TempDir Path tempDir) throws Exception {
        Path staged = tempDir.resolve("cross-department.pdf");
        Files.write(staged, new byte[]{1});
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(analyzedIntake(staged)));
        doThrow(new AccessDeniedException("无权修改非本人或非本部门案件"))
                .when(caseService).assertCaseEditable(7L, 3L);

        assertThrows(AccessDeniedException.class, () -> service.confirm(10L, baseRequest(), 3L));

        assertTrue(Files.exists(staged));
        verifyNoInteractions(caseDocumentService, calendarService, reminderService, todoService);
    }

    @Test
    void confirmedDatesCreateCalendarTodoAndAuditedActivities(@TempDir Path tempDir) throws Exception {
        Path staged = tempDir.resolve("传票.pdf");
        Files.write(staged, new byte[]{1, 2, 3});
        AIDocumentIntake intake = analyzedIntake(staged);
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(intake));
        when(intakeRepository.save(any(AIDocumentIntake.class))).thenAnswer(call -> call.getArgument(0));

        Case targetCase = new Case();
        targetCase.setId(7L);
        targetCase.setCaseName("测试案件");
        when(caseRepository.findById(7L)).thenReturn(Optional.of(targetCase));

        CaseDocumentDTO document = new CaseDocumentDTO();
        document.setId(55L);
        when(caseDocumentService.uploadDocument(eq(7L), any(), eq("传票"), eq("03_法律文书"),
                eq(3L), any(), eq("hash-1"))).thenReturn(document);
        CalendarDTO savedCalendar = new CalendarDTO();
        savedCalendar.setId(70L);
        when(calendarService.createCalendar(any(CalendarDTO.class), eq(3L))).thenReturn(savedCalendar);
        TodoDTO savedTodo = new TodoDTO();
        savedTodo.setId(80L);
        when(todoService.createTodo(any(TodoDTO.class), eq(3L))).thenReturn(savedTodo);

        AIDocumentIntakeConfirmRequest request = baseRequest();
        request.setCreateHearingCalendar(true);
        request.setHearingTime(LocalDateTime.of(2099, 8, 10, 9, 30));
        request.setHearingLocation("第三审判庭");
        request.setCreateDeadlineTodo(true);
        request.setDeadlineTime(LocalDateTime.of(2099, 8, 20, 18, 0));
        request.setDeadlineTitle("提交书面答辩");

        AIDocumentIntakeDTO result = service.confirm(10L, request, 3L);

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("文件已归入案件并登记进展，并同步创建2项日程或待办", result.getMessage());
        assertFalse(Files.exists(staged));
        verify(caseService).assertCaseEditable(7L, 3L);
        verify(calendarService).createCalendar(any(CalendarDTO.class), eq(3L));
        verify(reminderService).scheduleUpcomingHearingReminders(70L, request.getHearingTime());
        verify(todoService).createTodo(any(TodoDTO.class), eq(3L));
        verify(activityService).create(eq(7L), eq("HEARING"), any(), any(), eq(request.getHearingTime()),
                eq("DOCUMENT"), eq(55L), eq(3L), any(), any());
        verify(activityService).create(eq(7L), eq("DEADLINE"), any(), any(), any(),
                eq("DOCUMENT"), eq(55L), eq(3L), any(), any());
    }

    @Test
    void incompleteConfirmedHearingIsRejectedBeforeFileUpload(@TempDir Path tempDir) throws Exception {
        Path staged = tempDir.resolve("传票.pdf");
        Files.write(staged, new byte[]{1});
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(analyzedIntake(staged)));
        AIDocumentIntakeConfirmRequest request = baseRequest();
        request.setCreateHearingCalendar(true);
        request.setHearingTime(LocalDateTime.of(2099, 8, 10, 9, 30));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.confirm(10L, request, 3L));

        assertEquals("请确认开庭地点", error.getMessage());
        assertTrue(Files.exists(staged));
        verifyNoInteractions(caseDocumentService, calendarService, reminderService, todoService);
    }

    @Test
    void cleanupExpiredRemovesFileAndSensitiveTransientContent(@TempDir Path tempDir) throws Exception {
        ReflectionTestUtils.setField(service, "tempDir", tempDir.toString());
        Path staged = tempDir.resolve("expired.pdf");
        Files.write(staged, new byte[]{1});
        AIDocumentIntake intake = analyzedIntake(staged);
        intake.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(intakeRepository.findTop200ByExpiresAtBeforeAndStatusInAndDeletedFalseOrderByExpiresAtAsc(
                any(), any())).thenReturn(java.util.List.of(intake));
        when(intakeRepository.save(any(AIDocumentIntake.class))).thenAnswer(call -> call.getArgument(0));

        int cleaned = service.cleanupExpired(LocalDateTime.now());

        assertEquals(1, cleaned);
        assertTrue(Files.notExists(staged));
        assertEquals("EXPIRED", intake.getStatus());
        assertEquals("", intake.getTempPath());
        assertNull(intake.getExtractedText());
        assertNull(intake.getAnalysisJson());
        assertNull(intake.getCandidatesJson());
    }

    @Test
    void cleanupRejectsPathOutsideConfiguredIntakeRoot(@TempDir Path tempDir) throws Exception {
        Path configuredRoot = tempDir.resolve("intake-root");
        Files.createDirectories(configuredRoot);
        ReflectionTestUtils.setField(service, "tempDir", configuredRoot.toString());
        Path outside = tempDir.resolve("must-not-delete.pdf");
        Files.write(outside, new byte[]{1});
        AIDocumentIntake intake = analyzedIntake(outside);
        intake.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(intakeRepository.findTop200ByExpiresAtBeforeAndStatusInAndDeletedFalseOrderByExpiresAtAsc(
                any(), any())).thenReturn(java.util.List.of(intake));
        when(intakeRepository.save(any(AIDocumentIntake.class))).thenAnswer(call -> call.getArgument(0));

        int cleaned = service.cleanupExpired(LocalDateTime.now());

        assertEquals(0, cleaned);
        assertTrue(Files.exists(outside));
        assertEquals("CLEANUP_REJECTED", intake.getStatus());
        assertEquals(outside.toString(), intake.getTempPath());
        assertNull(intake.getExtractedText());
    }

    @Test
    void confirmingExpiredIntakeCleansItBeforeReturningError(@TempDir Path tempDir) throws Exception {
        ReflectionTestUtils.setField(service, "tempDir", tempDir.toString());
        Path staged = tempDir.resolve("expired-confirm.pdf");
        Files.write(staged, new byte[]{1});
        AIDocumentIntake intake = analyzedIntake(staged);
        intake.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(intake));
        when(intakeRepository.save(any(AIDocumentIntake.class))).thenAnswer(call -> call.getArgument(0));

        DocumentIntakeExpiredException error = assertThrows(DocumentIntakeExpiredException.class,
                () -> service.confirm(10L, baseRequest(), 3L));

        assertEquals("待确认文件已过期并进入清理流程", error.getMessage());
        assertTrue(Files.notExists(staged));
        assertEquals("EXPIRED", intake.getStatus());
        verifyNoInteractions(caseDocumentService, calendarService, reminderService, todoService);
    }

    @Test
    void confirmedTempFileIsDeletedOnlyAfterTransactionCommit(@TempDir Path tempDir) throws Exception {
        Path staged = tempDir.resolve("commit-safe.pdf");
        Files.write(staged, new byte[]{1});
        AIDocumentIntake intake = analyzedIntake(staged);
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(intake));
        when(intakeRepository.save(any(AIDocumentIntake.class))).thenAnswer(call -> call.getArgument(0));
        CaseDocumentDTO document = new CaseDocumentDTO();
        document.setId(55L);
        when(caseDocumentService.uploadDocument(eq(7L), any(), eq("传票"), eq("03_法律文书"),
                eq(3L), any(), eq("hash-1"))).thenReturn(document);
        AIDocumentIntakeConfirmRequest request = baseRequest();
        request.setRegisterActivity(false);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.confirm(10L, request, 3L);
            assertTrue(Files.exists(staged));

            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
            assertTrue(Files.notExists(staged));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private AIDocumentIntakeConfirmRequest baseRequest() {
        AIDocumentIntakeConfirmRequest request = new AIDocumentIntakeConfirmRequest();
        request.setCaseId(7L);
        request.setFolderPath("03_法律文书");
        request.setDocumentType("传票");
        request.setRegisterActivity(true);
        return request;
    }

    private AIDocumentIntake analyzedIntake(Path path) {
        AIDocumentIntake intake = new AIDocumentIntake();
        intake.setId(10L);
        intake.setOriginalFileName("传票.pdf");
        intake.setTempPath(path.toString());
        intake.setMimeType("application/pdf");
        intake.setFileSize(3L);
        intake.setContentSha256("hash-1");
        intake.setExtractedText("测试文字");
        intake.setAnalysisJson("{}");
        intake.setCandidatesJson("[]");
        intake.setUploadBy(3L);
        intake.setStatus("ANALYZED");
        intake.setExpiresAt(LocalDateTime.now().plusDays(1));
        return intake;
    }
}
