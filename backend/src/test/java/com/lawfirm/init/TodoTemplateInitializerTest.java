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
import java.util.Map;
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

        Map<String, Set<String>> actualStages = templates.stream().collect(Collectors.groupingBy(
                StageTodoTemplate::getCaseType,
                Collectors.mapping(StageTodoTemplate::getStageName, Collectors.toSet())
        ));
        Map<String, Set<String>> expectedStages = Map.of(
                "CIVIL", Set.of("接洽利冲", "签约立案", "诉前准备", "立案或应诉", "举证答辩", "庭审", "裁判", "后续程序", "结案归档"),
                "ARBITRATION", Set.of("接洽利冲", "签约立案", "仲裁条款审查", "申请或答辩", "组庭", "举证", "开庭", "裁决", "执行衔接", "结案归档"),
                "CRIMINAL", Set.of("接洽利冲", "签约", "侦查与会见", "审查起诉", "阅卷", "一审", "二审或申诉", "结案归档"),
                "ADMINISTRATIVE", Set.of("接洽利冲", "签约立案", "行政行为审查", "复议或起诉", "举证", "庭审", "裁判", "后续程序", "结案归档"),
                "NON_LITIGATION", Set.of("接洽利冲", "签约立项", "资料收集", "调查核验", "起草或谈判", "内部复核", "成果交付", "整改跟踪", "项目归档"),
                "CONSULTANT", Set.of("顾问建档", "服务计划", "需求受理", "分派办理", "审核交付", "定期报告", "续签评估", "终止或归档")
        );
        assertEquals(expectedStages, actualStages);
    }
}
