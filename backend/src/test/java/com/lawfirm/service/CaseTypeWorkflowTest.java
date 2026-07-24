package com.lawfirm.service;

import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseStage;
import com.lawfirm.entity.StageTodoTemplate;
import com.lawfirm.entity.Todo;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.CaseStageRepository;
import com.lawfirm.repository.StageTodoTemplateRepository;
import com.lawfirm.repository.TodoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class CaseTypeWorkflowTest {
    @Mock private CaseStageRepository caseStageRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private CaseTimelineService caseTimelineService;
    @Mock private TodoRepository todoRepository;
    @Mock private StageTodoTemplateRepository stageTodoTemplateRepository;
    @InjectMocks private CaseStageService service;

    @SuppressWarnings("unchecked")
    @Test
    void consultantHasContinuousServiceWorkflow() {
        List<String> stages = ReflectionTestUtils.invokeMethod(service, "getStagesByCaseType", "CONSULTANT");
        assertEquals("顾问建档", stages.get(0));
        assertTrue(stages.contains("需求受理"));
        assertTrue(stages.contains("审核交付"));
        assertEquals("终止或归档", stages.get(stages.size() - 1));
    }

    @SuppressWarnings("unchecked")
    @Test
    void arbitrationHasIndependentWorkflow() {
        List<String> stages = ReflectionTestUtils.invokeMethod(service, "getStagesByCaseType", "ARBITRATION");
        assertTrue(stages.contains("仲裁条款审查"));
        assertTrue(stages.contains("组庭"));
        assertTrue(stages.contains("裁决"));
    }

    @Test
    void allSupportedCaseTypesHaveSpecificWorkflow() {
        for (String caseType : List.of("CIVIL", "ARBITRATION", "CRIMINAL", "ADMINISTRATIVE", "NON_LITIGATION", "CONSULTANT")) {
            List<String> stages = ReflectionTestUtils.invokeMethod(service, "getStagesByCaseType", caseType);
            assertTrue(stages.size() >= 8, caseType + " should have a complete workflow");
        }
    }

    @Test
    void stageTodoIsAssignedToCaseOwner() {
        Case caseEntity = new Case();
        caseEntity.setId(9L);
        caseEntity.setOwnerId(21L);
        caseEntity.setCaseType("CONSULTANT");

        StageTodoTemplate template = new StageTodoTemplate();
        template.setTodoTitle("登记顾问需求");
        template.setTodoDescription("记录事项和期限");
        template.setPriority(1);
        template.setRelativeDays(0);

        when(caseRepository.findById(9L)).thenReturn(Optional.of(caseEntity));
        when(stageTodoTemplateRepository.findByStageNameAndCaseTypeAndIsEnabledAndIsDeletedFalseOrderBySortOrderAsc(
                "需求受理", "CONSULTANT", true)).thenReturn(List.of(template));

        service.autoCreateTodos(9L, "需求受理");

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        assertEquals(21L, captor.getValue().getAssigneeId());
        assertEquals(9L, captor.getValue().getCaseId());
        assertEquals("HIGH", captor.getValue().getPriority());
    }

    @Test
    void stageCannotAdvanceBeforeFilingApproval() {
        Case caseEntity = new Case();
        caseEntity.setId(10L);
        caseEntity.setStatus("PENDING_APPROVAL");
        when(caseRepository.findById(10L)).thenReturn(Optional.of(caseEntity));

        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> service.changeStatus(10L, "签约立案", "提前推进", 21L));

        assertEquals("案件立案审批通过且处于办理中时才能变更办理阶段", error.getMessage());
        verifyNoInteractions(caseStageRepository, todoRepository);
    }

    @Test
    void lawyerCanSkipStagesWithReasonAndAuditTrail() {
        Case caseEntity = new Case();
        caseEntity.setId(15L);
        caseEntity.setOwnerId(21L);
        caseEntity.setCaseType("CIVIL");
        caseEntity.setStatus("ACTIVE");
        CaseStage current = stage(201L, 15L, "接洽利冲", 1, "IN_PROGRESS");
        CaseStage skipped = stage(202L, 15L, "签约立案", 2, "PENDING");
        CaseStage target = stage(203L, 15L, "诉前准备", 3, "PENDING");
        when(caseRepository.findById(15L)).thenReturn(Optional.of(caseEntity));
        when(caseStageRepository.findCurrentStage(15L)).thenReturn(Optional.of(current));
        when(caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(15L))
                .thenReturn(List.of(current, skipped, target));
        when(stageTodoTemplateRepository.findByStageNameAndCaseTypeAndIsEnabledAndIsDeletedFalseOrderBySortOrderAsc(
                "诉前准备", "CIVIL", true)).thenReturn(List.of());

        service.changeStatus(15L, "诉前准备", "案件已完成前期签约", 21L);

        assertEquals("COMPLETED", current.getStatus());
        assertEquals("SKIPPED", skipped.getStatus());
        assertEquals("IN_PROGRESS", target.getStatus());
        assertEquals("诉前准备", caseEntity.getCurrentStage());
        verify(caseTimelineService).createSystemTimeline(eq(15L), eq("STAGE_CHANGED"),
                contains("跳过阶段：签约立案"), eq(21L));
    }

    @Test
    void skippedStageRequiresReason() {
        Case caseEntity = new Case();
        caseEntity.setId(16L);
        caseEntity.setStatus("ACTIVE");
        CaseStage current = stage(301L, 16L, "接洽利冲", 1, "IN_PROGRESS");
        CaseStage middle = stage(302L, 16L, "签约立案", 2, "PENDING");
        CaseStage target = stage(303L, 16L, "诉前准备", 3, "PENDING");
        when(caseRepository.findById(16L)).thenReturn(Optional.of(caseEntity));
        when(caseStageRepository.findCurrentStage(16L)).thenReturn(Optional.of(current));
        when(caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(16L))
                .thenReturn(List.of(current, middle, target));

        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> service.changeStatus(16L, "诉前准备", " ", 21L));

        assertTrue(error.getMessage().contains("跳过案件阶段时必须填写原因"));
    }

    @Test
    void nextStageLookupUsesStableStageIdentityInsteadOfObjectEquality() {
        CaseStage currentFromQuery = stage(101L, 11L, "接洽利冲", 1, "IN_PROGRESS");
        CaseStage currentFromList = stage(101L, 11L, "接洽利冲", 1, "IN_PROGRESS");
        currentFromList.setStartDate(java.time.LocalDate.now());
        CaseStage next = stage(102L, 11L, "签约立案", 2, "PENDING");
        when(caseStageRepository.findCurrentStage(11L)).thenReturn(Optional.of(currentFromQuery));
        when(caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(11L))
                .thenReturn(List.of(currentFromList, next));

        assertEquals(Optional.of("签约立案"), service.getNextStageName(11L));
    }

    @Test
    void historicalCommercialCaseUsesArbitrationTodoTemplates() {
        Case caseEntity = new Case();
        caseEntity.setId(13L);
        caseEntity.setOwnerId(21L);
        caseEntity.setCaseType("COMMERCIAL");
        when(caseRepository.findById(13L)).thenReturn(Optional.of(caseEntity));
        when(stageTodoTemplateRepository.findByStageNameAndCaseTypeAndIsEnabledAndIsDeletedFalseOrderBySortOrderAsc(
                "仲裁条款审查", "ARBITRATION", true)).thenReturn(List.of());

        service.autoCreateTodos(13L, "仲裁条款审查");

        verify(stageTodoTemplateRepository)
                .findByStageNameAndCaseTypeAndIsEnabledAndIsDeletedFalseOrderBySortOrderAsc(
                        "仲裁条款审查", "ARBITRATION", true);
    }

    @Test
    void todoPersistenceFailureIsNotSwallowedDuringStageTransition() {
        Case caseEntity = new Case();
        caseEntity.setId(14L);
        caseEntity.setOwnerId(21L);
        caseEntity.setCaseType("CRIMINAL");
        StageTodoTemplate template = new StageTodoTemplate();
        template.setTodoTitle("完成刑事委托手续");
        template.setPriority(1);
        template.setRelativeDays(1);
        when(caseRepository.findById(14L)).thenReturn(Optional.of(caseEntity));
        when(stageTodoTemplateRepository.findByStageNameAndCaseTypeAndIsEnabledAndIsDeletedFalseOrderBySortOrderAsc(
                "签约", "CRIMINAL", true)).thenReturn(List.of(template));
        when(todoRepository.save(any(Todo.class))).thenThrow(new IllegalStateException("todo storage unavailable"));

        assertThrows(IllegalStateException.class, () -> service.autoCreateTodos(14L, "签约"));
    }

    @Test
    void pendingConsultantCaseWithLegacyStagesIsSafelyMigrated() {
        Case caseEntity = new Case();
        caseEntity.setId(12L);
        caseEntity.setOwnerId(21L);
        caseEntity.setCaseType("CONSULTANT");
        caseEntity.setStatus("PENDING_APPROVAL");
        caseEntity.setCurrentStage("咨询");

        CaseStage legacy = new CaseStage();
        legacy.setId(88L);
        legacy.setCaseId(12L);
        legacy.setStageName("咨询");
        legacy.setStageOrder(1);
        legacy.setStatus("IN_PROGRESS");
        legacy.setDeleted(false);

        when(caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(12L))
                .thenReturn(List.of(legacy));
        when(caseRepository.findById(12L)).thenReturn(Optional.of(caseEntity));

        assertTrue(service.reconcilePendingApprovalWorkflow(caseEntity));
        assertTrue(legacy.getDeleted());
        assertEquals("顾问建档", caseEntity.getCurrentStage());
        verify(caseTimelineService).createSystemTimeline(eq(12L), eq("WORKFLOW_MIGRATED"), anyString());
    }

    private CaseStage stage(Long id, Long caseId, String name, int order, String status) {
        CaseStage stage = new CaseStage();
        stage.setId(id);
        stage.setCaseId(caseId);
        stage.setStageName(name);
        stage.setStageOrder(order);
        stage.setStatus(status);
        stage.setDeleted(false);
        return stage;
    }
}
