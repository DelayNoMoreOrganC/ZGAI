package com.lawfirm.service;

import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseStage;
import com.lawfirm.entity.StageTodoTemplate;
import com.lawfirm.entity.Todo;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.CaseStageRepository;
import com.lawfirm.repository.StageTodoTemplateRepository;
import com.lawfirm.repository.TodoRepository;
import com.lawfirm.vo.CaseDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 案件阶段服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseStageService {

    private final CaseStageRepository caseStageRepository;
    private final CaseRepository caseRepository;
    private final CaseTimelineService caseTimelineService;
    private final TodoRepository todoRepository;
    private final StageTodoTemplateRepository stageTodoTemplateRepository;

    /**
     * 初始化案件阶段（根据案件类型创建标准流程）
     */
    @Transactional
    public void initializeStages(Long caseId, String caseType) {
        List<String> stages = getStagesByCaseType(caseType);

        for (int i = 0; i < stages.size(); i++) {
            CaseStage stage = new CaseStage();
            stage.setCaseId(caseId);
            stage.setStageName(stages.get(i));
            stage.setStageOrder(i + 1);
            stage.setStatus(i == 0 ? "IN_PROGRESS" : "PENDING");
            stage.setDeleted(false);
            caseStageRepository.save(stage);
        }

        // 更新案件当前阶段
        Case caseEntity = caseRepository.findById(caseId).orElseThrow();
        caseEntity.setCurrentStage(stages.get(0));
        caseRepository.save(caseEntity);
    }

    /**
     * 变更案件阶段状态
     */
    @Transactional
    public void changeStatus(Long caseId, String targetStage, String reason, Long operatorId) {
        // 1. 查找当前阶段
        Optional<CaseStage> currentStageOpt = caseStageRepository.findCurrentStage(caseId);
        if (currentStageOpt.isEmpty()) {
            throw new RuntimeException("未找到当前进行中的阶段");
        }

        CaseStage currentStage = currentStageOpt.get();

        // 2. 查找目标阶段
        List<CaseStage> allStages = caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(caseId);
        CaseStage targetStageEntity = allStages.stream()
                .filter(s -> s.getStageName().equals(targetStage))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("目标阶段不存在"));

        // 3. 验证阶段流转是否合法
        if (!isValidTransition(currentStage, targetStageEntity, allStages)) {
            throw new RuntimeException("不允许从当前阶段直接跳转到目标阶段");
        }

        // 4. 更新当前阶段状态
        currentStage.setStatus("COMPLETED");
        currentStage.setEndDate(LocalDate.now());
        caseStageRepository.save(currentStage);

        // 5. 更新目标阶段状态
        targetStageEntity.setStatus("IN_PROGRESS");
        targetStageEntity.setStartDate(LocalDate.now());
        caseStageRepository.save(targetStageEntity);

        // 6. 更新案件当前阶段
        Case caseEntity = caseRepository.findById(caseId).orElseThrow();
        caseEntity.setCurrentStage(targetStage);
        caseRepository.save(caseEntity);

        // 7. 记录动态
        caseTimelineService.createSystemTimeline(
                caseId,
                "STAGE_CHANGED",
                String.format("案件阶段从「%s」变更为「%s」。原因：%s",
                        currentStage.getStageName(), targetStage, reason != null ? reason : "正常流转")
        );

        // 8. 自动创建该阶段的待办事项
        autoCreateTodos(caseId, targetStage);
    }

    /**
     * 自动创建待办事项
     */
    @Transactional
    public void autoCreateTodos(Long caseId, String stageName) {
        try {
            // 获取案件信息
            Case caseEntity = caseRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("案件不存在"));

            // 获取该阶段和案件类型的待办模板
            List<StageTodoTemplate> templates = stageTodoTemplateRepository
                    .findByStageNameAndCaseTypeAndIsEnabledAndIsDeletedFalseOrderBySortOrderAsc(
                            stageName, caseEntity.getCaseType(), true
                    );

            if (templates.isEmpty()) {
                log.info("案件 {} 的阶段 {} 没有配置待办模板", caseId, stageName);
                return;
            }

            // 为每个模板创建待办事项
            int createdCount = 0;
            for (StageTodoTemplate template : templates) {
                Todo todo = new Todo();
                todo.setTitle(template.getTodoTitle());
                todo.setDescription(template.getTodoDescription());
                todo.setCaseId(caseId);
                todo.setPriority(mapPriorityFromTemplate(template.getPriority()));
                todo.setStatus("PENDING");
                todo.setDeleted(false);

                // 计算到期日期（LocalDate转LocalDateTime，设为当天23:59）
                LocalDate dueDate = LocalDate.now().plusDays(template.getRelativeDays());
                todo.setDueDate(dueDate.atTime(23, 59, 59));

                todoRepository.save(todo);
                createdCount++;
            }

            log.info("为案件 {} 的阶段 {} 自动创建了 {} 个待办事项", caseId, stageName, createdCount);

        } catch (Exception e) {
            log.error("自动创建待办事项失败: caseId={}, stageName={}", caseId, stageName, e);
            // 不抛出异常，避免影响状态变更流程
        }
    }

    /**
     * 映射优先级（从模板的数字格式到待办的字符串格式）
     */
    private String mapPriorityFromTemplate(Integer templatePriority) {
        if (templatePriority == null) {
            return "MEDIUM";
        }
        switch (templatePriority) {
            case 1:
                return "HIGH";
            case 2:
                return "MEDIUM";
            case 3:
                return "LOW";
            default:
                return "MEDIUM";
        }
    }

    /**
     * 获取状态历史
     */
    public List<StageHistoryVO> getStatusHistory(Long caseId) {
        List<CaseStage> stages = caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(caseId);

        List<StageHistoryVO> history = new ArrayList<>();
        for (CaseStage stage : stages) {
            StageHistoryVO vo = new StageHistoryVO();
            vo.setStageName(stage.getStageName());
            vo.setStageOrder(stage.getStageOrder());
            vo.setStatus(stage.getStatus());
            vo.setStartDate(stage.getStartDate());
            vo.setEndDate(stage.getEndDate());
            history.add(vo);
        }

        return history;
    }

    /**
     * 回退阶段状态
     */
    @Transactional
    public void rollbackStatus(Long caseId, String targetStage, String reason, Long operatorId) {
        // 1. 查找当前阶段
        Optional<CaseStage> currentStageOpt = caseStageRepository.findCurrentStage(caseId);
        if (currentStageOpt.isEmpty()) {
            throw new RuntimeException("未找到当前进行中的阶段");
        }

        CaseStage currentStage = currentStageOpt.get();

        // 2. 查找目标阶段
        List<CaseStage> allStages = caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(caseId);
        CaseStage targetStageEntity = allStages.stream()
                .filter(s -> s.getStageName().equals(targetStage))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("目标阶段不存在"));

        // 3. 验证是否可以回退（只能回退到已完成或进行中的阶段）
        if (!targetStageEntity.getStatus().equals("COMPLETED") &&
            !targetStageEntity.getStatus().equals("IN_PROGRESS")) {
            throw new RuntimeException("只能回退到已完成或进行中的阶段");
        }

        // 4. 更新当前阶段状态
        currentStage.setStatus("PENDING");
        currentStage.setStartDate(null);
        currentStage.setEndDate(null);
        caseStageRepository.save(currentStage);

        // 5. 如果目标阶段是已完成状态，则重新激活
        if (targetStageEntity.getStatus().equals("COMPLETED")) {
            targetStageEntity.setStatus("IN_PROGRESS");
            targetStageEntity.setEndDate(null);
            caseStageRepository.save(targetStageEntity);
        }

        // 6. 更新案件当前阶段
        Case caseEntity = caseRepository.findById(caseId).orElseThrow();
        caseEntity.setCurrentStage(targetStage);
        caseRepository.save(caseEntity);

        // 7. 记录动态
        caseTimelineService.createSystemTimeline(
                caseId,
                "STAGE_ROLLBACK",
                String.format("案件阶段从「%s」回退到「%s」。原因：%s",
                        currentStage.getStageName(), targetStage, reason)
        );
    }

    /**
     * 获取阶段进度（用于详情页展示）
     */
    public List<CaseDetailVO.StageProgressVO> getStageProgress(Long caseId) {
        List<CaseStage> stages = caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(caseId);

        return stages.stream()
                .map(stage -> {
                    CaseDetailVO.StageProgressVO vo = new CaseDetailVO.StageProgressVO();
                    vo.setStageName(stage.getStageName());
                    vo.setStageOrder(stage.getStageOrder());
                    vo.setStatus(stage.getStatus());
                    vo.setStatusDesc(getStatusDesc(stage.getStatus()));
                    vo.setStartDate(stage.getStartDate());
                    vo.setEndDate(stage.getEndDate());
                    return vo;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // 辅助方法

    /**
     * 根据案件类型获取标准阶段流程
     */
    private List<String> getStagesByCaseType(String caseType) {
        switch (caseType) {
            case "CIVIL":
                return List.of("咨询", "签约", "起草文书", "待立案", "已立案", "一审审理中", "一审结案", "执行", "结案归档");
            case "CRIMINAL":
                return List.of("咨询", "签约", "会见", "审查起诉", "一审", "二审", "结案");
            case "ADMINISTRATIVE":
                return List.of("咨询", "签约", "起草文书", "待立案", "已立案", "一审", "二审", "结案");
            case "COMMERCIAL":
            case "ARBITRATION":
                return List.of("咨询", "签约", "起草文书", "申请仲裁", "组庭", "开庭", "裁决", "结案");
            case "NON_LITIGATION":
                return List.of("咨询", "签约", "尽职调查", "出具文书", "交付", "结案");
            default:
                return List.of("咨询", "签约", "办理", "结案");
        }
    }

    /**
     * 验证阶段流转是否合法
     */
    private boolean isValidTransition(CaseStage currentStage, CaseStage targetStage, List<CaseStage> allStages) {
        // 允许进入下一个阶段
        int currentIndex = allStages.indexOf(currentStage);
        int targetIndex = allStages.indexOf(targetStage);

        return targetIndex == currentIndex + 1;
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(String status) {
        if (status == null) return null;
        switch (status) {
            case "PENDING": return "待开始";
            case "IN_PROGRESS": return "进行中";
            case "COMPLETED": return "已完成";
            default: return status;
        }
    }

    /**
     * 阶段历史VO
     */
    @lombok.Data
    public static class StageHistoryVO {
        private String stageName;
        private Integer stageOrder;
        private String status;
        private String statusDesc;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
