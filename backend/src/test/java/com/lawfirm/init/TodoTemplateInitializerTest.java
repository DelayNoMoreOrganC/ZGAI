package com.lawfirm.init;

import com.lawfirm.entity.StageTodoTemplate;
import com.lawfirm.repository.StageTodoTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoTemplateInitializerTest {

    @Mock
    private StageTodoTemplateRepository repository;

    @InjectMocks
    private TodoTemplateInitializer initializer;

    @Test
    void seedsEverySupportedCaseTypeWithRuntimeCodes() {
        when(repository.existsByCaseTypeAndStageNameAndTodoTitleAndIsDeletedFalse(
                anyString(), anyString(), anyString())).thenReturn(false);

        initializer.run();

        ArgumentCaptor<StageTodoTemplate> captor = ArgumentCaptor.forClass(StageTodoTemplate.class);
        verify(repository, org.mockito.Mockito.atLeast(48)).save(captor.capture());
        List<StageTodoTemplate> templates = captor.getAllValues();
        Set<String> caseTypes = templates.stream().map(StageTodoTemplate::getCaseType).collect(Collectors.toSet());

        assertEquals(Set.of("CIVIL", "ARBITRATION", "CRIMINAL", "ADMINISTRATIVE", "NON_LITIGATION", "CONSULTANT"), caseTypes);
        assertTrue(templates.stream().anyMatch(item -> "CONSULTANT".equals(item.getCaseType())
                && "需求受理".equals(item.getStageName())));
        assertTrue(templates.stream().allMatch(item -> item.getTodoTitle() != null && !item.getTodoTitle().isBlank()));
    }
}
