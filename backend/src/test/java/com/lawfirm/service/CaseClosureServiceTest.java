package com.lawfirm.service;

import com.lawfirm.dto.CaseClosureCreateRequest;
import com.lawfirm.entity.*;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseClosureServiceTest {
    @Mock private CaseClosureRequestRepository closureRequestRepository;
    @Mock private CaseClosureDocumentRepository closureDocumentRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private CaseStageRepository caseStageRepository;
    @Mock private CaseDocumentRepository caseDocumentRepository;
    @Mock private ApprovalRepository approvalRepository;
    @Mock private ApprovalFlowRepository approvalFlowRepository;
    @Mock private UserRepository userRepository;
    @Mock private TodoRepository todoRepository;
    @Mock private UserPermissionService userPermissionService;
    @Mock private CaseService caseService;
    @Mock private ObjectProvider<CaseService> caseServiceProvider;
    @Mock private NotificationService notificationService;
    @Mock private CaseTimelineService caseTimelineService;

    private CaseClosureService service;

    @BeforeEach
    void setUp() {
        service = new CaseClosureService(
                closureRequestRepository, closureDocumentRepository, caseRepository, caseStageRepository,
                caseDocumentRepository, approvalRepository, approvalFlowRepository, userRepository,
                todoRepository, userPermissionService, caseServiceProvider, notificationService, caseTimelineService);
        lenient().when(caseServiceProvider.getObject()).thenReturn(caseService);
    }

    @Test
    void createsStructuredAdministrativeReviewFromFinalStageAndCaseDocuments() {
        Case caseEntity = activeCase();
        CaseStage finalStage = stage(9L, 9, "结案归档");
        CaseDocument judgment = document(31L, 8L, "民事判决书.pdf");
        User administrative = user(3L, "验收行政");
        when(caseRepository.findById(8L)).thenReturn(Optional.of(caseEntity));
        when(caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(8L)).thenReturn(List.of(finalStage));
        when(caseStageRepository.findCurrentStage(8L)).thenReturn(Optional.of(finalStage));
        when(closureRequestRepository.findByCaseIdAndStatusAndDeletedFalse(8L, "PENDING"))
                .thenReturn(Collections.emptyList());
        when(caseDocumentRepository.findAllById(List.of(31L))).thenReturn(List.of(judgment));
        when(userPermissionService.findFirstActiveUserByPermission(eq("CASE_ARCHIVE_REVIEW"), anyList()))
                .thenReturn(Optional.of(administrative));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> {
            Approval approval = invocation.getArgument(0);
            approval.setId(40L);
            return approval;
        });
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
            Todo todo = invocation.getArgument(0);
            todo.setId(45L);
            return todo;
        });
        when(closureRequestRepository.save(any(CaseClosureRequest.class))).thenAnswer(invocation -> {
            CaseClosureRequest closure = invocation.getArgument(0);
            closure.setId(50L);
            return closure;
        });
        when(closureDocumentRepository.findByClosureRequestIdAndDeletedFalseOrderByIdAsc(50L))
                .thenReturn(Collections.emptyList());

        var result = service.create(8L, validRequest(), 2L);

        assertEquals(40L, result.getApprovalId());
        ArgumentCaptor<Approval> approvalCaptor = ArgumentCaptor.forClass(Approval.class);
        verify(approvalRepository).save(approvalCaptor.capture());
        assertEquals(CaseClosureService.APPROVAL_TYPE, approvalCaptor.getValue().getApprovalType());
        assertEquals(3L, approvalCaptor.getValue().getCurrentApproverId());
        assertTrue(approvalCaptor.getValue().getContent().contains("民事判决书.pdf"));
        verify(closureDocumentRepository).save(argThat(link ->
                link.getClosureRequestId().equals(50L) && link.getCaseDocumentId().equals(31L)));
        verify(notificationService).sendNotification(eq(3L), contains("结案"), anyString(),
                eq(NotificationService.CATEGORY_APPROVAL), eq(40L), eq("APPROVAL_PENDING"));
        verify(todoRepository).save(argThat(todo -> todo.getAssigneeId().equals(3L)
                && todo.getCaseId().equals(8L) && "IMPORTANT".equals(todo.getPriority())));
    }

    @Test
    void rejectsClosureBeforeLastWorkflowStage() {
        Case caseEntity = activeCase();
        CaseStage current = stage(1L, 1, "接洽利冲");
        CaseStage last = stage(9L, 9, "结案归档");
        when(caseRepository.findById(8L)).thenReturn(Optional.of(caseEntity));
        when(caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(8L)).thenReturn(List.of(current, last));
        when(caseStageRepository.findCurrentStage(8L)).thenReturn(Optional.of(current));

        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> service.create(8L, validRequest(), 2L));

        assertTrue(error.getMessage().contains("最后办理阶段"));
        verifyNoInteractions(approvalRepository);
    }

    @Test
    void approvalClosesCaseAndCompletesFinalStageTogether() {
        Case caseEntity = activeCase();
        CaseStage finalStage = stage(9L, 9, "结案归档");
        CaseClosureRequest closure = new CaseClosureRequest();
        closure.setId(50L);
        closure.setCaseId(8L);
        closure.setApprovalId(40L);
        closure.setClosureType("JUDGMENT");
        closure.setStatus("PENDING");
        when(closureRequestRepository.findByApprovalIdAndDeletedFalse(40L)).thenReturn(Optional.of(closure));
        when(caseRepository.findById(8L)).thenReturn(Optional.of(caseEntity));
        when(caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(8L)).thenReturn(List.of(finalStage));
        when(caseStageRepository.findCurrentStage(8L)).thenReturn(Optional.of(finalStage));
        Todo reviewTodo = new Todo();
        reviewTodo.setId(45L);
        reviewTodo.setStatus("PENDING");
        closure.setReviewTodoId(45L);
        when(todoRepository.findById(45L)).thenReturn(Optional.of(reviewTodo));
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 7, 24, 15, 30);

        service.approve(40L, 3L, reviewedAt, "材料齐全，同意结案");

        assertEquals("APPROVED", closure.getStatus());
        assertEquals("CLOSED", caseEntity.getStatus());
        assertEquals(reviewedAt.toLocalDate(), caseEntity.getCloseDate());
        assertEquals("COMPLETED", finalStage.getStatus());
        assertEquals(reviewedAt.toLocalDate(), finalStage.getEndDate());
        verify(caseRepository).save(caseEntity);
        verify(caseStageRepository).save(finalStage);
        assertEquals("COMPLETED", reviewTodo.getStatus());
        verify(todoRepository).save(reviewTodo);
    }

    @Test
    void rejectsDocumentFromAnotherCase() {
        Case caseEntity = activeCase();
        CaseStage finalStage = stage(9L, 9, "结案归档");
        when(caseRepository.findById(8L)).thenReturn(Optional.of(caseEntity));
        when(caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(8L)).thenReturn(List.of(finalStage));
        when(caseStageRepository.findCurrentStage(8L)).thenReturn(Optional.of(finalStage));
        when(closureRequestRepository.findByCaseIdAndStatusAndDeletedFalse(8L, "PENDING"))
                .thenReturn(Collections.emptyList());
        when(caseDocumentRepository.findAllById(List.of(31L)))
                .thenReturn(List.of(document(31L, 99L, "其他案件判决书.pdf")));

        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> service.create(8L, validRequest(), 2L));

        assertTrue(error.getMessage().contains("不属于当前案件"));
        verifyNoInteractions(approvalRepository);
    }

    private CaseClosureCreateRequest validRequest() {
        CaseClosureCreateRequest request = new CaseClosureCreateRequest();
        request.setClosureType("JUDGMENT");
        request.setCaseOutcome("法院判决支持主要诉讼请求");
        request.setClosureSummary("案件已经完成一审裁判及客户告知，后续履行事项已向客户作出书面说明。");
        request.setFeeStatus("SETTLED");
        request.setClientDeliveryStatus("COMPLETED");
        request.setClientDeliveryNotes("判决书已通过电子邮件交付客户并完成结果说明");
        request.setDocumentsConfirmed(true);
        request.setBasisDocumentIds(List.of(31L));
        return request;
    }

    private Case activeCase() {
        Case value = new Case();
        value.setId(8L);
        value.setCaseName("测试民事案件");
        value.setCaseNumber("ZGAI-2026-008");
        value.setStatus("ACTIVE");
        value.setDeleted(false);
        return value;
    }

    private CaseStage stage(Long id, int order, String name) {
        CaseStage value = new CaseStage();
        value.setId(id);
        value.setCaseId(8L);
        value.setStageOrder(order);
        value.setStageName(name);
        value.setStatus("IN_PROGRESS");
        value.setDeleted(false);
        return value;
    }

    private CaseDocument document(Long id, Long caseId, String name) {
        CaseDocument value = new CaseDocument();
        value.setId(id);
        value.setCaseId(caseId);
        value.setOriginalFileName(name);
        value.setDocumentType("裁判文书");
        value.setFolderPath("03_法律文书");
        value.setDeleted(false);
        return value;
    }

    private User user(Long id, String name) {
        User value = new User();
        value.setId(id);
        value.setRealName(name);
        value.setStatus(1);
        value.setDeleted(false);
        return value;
    }
}
