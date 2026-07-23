package com.lawfirm.service;

import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseStage;
import com.lawfirm.entity.StageTodoTemplate;
import com.lawfirm.entity.Todo;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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
}
