package com.lawfirm.schedule;

import com.lawfirm.entity.Todo;
import com.lawfirm.entity.Case;
import com.lawfirm.repository.TodoRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 逾期预警定时任务
 * PRD要求（436行）：审限前7天橙色/3天红色+通知/当天红色+推送/逾期置顶
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueWarningSchedule {

    private final TodoRepository todoRepository;
    private final CaseRepository caseRepository;
    private final NotificationService notificationService;

    /**
     * 每天凌晨1点执行逾期预警检查
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkOverdueWarnings() {
        log.info("开始执行逾期预警检查...");
        int updatedCount = 0;

        try {
            // 1. 检查待办事项逾期预警
            List<Todo> todos = todoRepository.findAll().stream()
                    .filter(t -> !t.getDeleted())
                    .filter(t -> "PENDING".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus()))
                    .filter(t -> t.getDueDate() != null)
                    .collect(java.util.stream.Collectors.toList());

            for (Todo todo : todos) {
                long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), todo.getDueDate());
                boolean updated = false;

                // 审限前7天：橙色预警
                if (daysUntilDue <= 7 && daysUntilDue > 3) {
                    if (!"ORANGE_WARNING".equals(todo.getPriority())) {
                        todo.setPriority("ORANGE_WARNING");
                        updated = true;
                        log.info("待办逾期预警（7天内）：ID={}, 标题={}, 截止日期={}",
                                todo.getId(), todo.getTitle(), todo.getDueDate());
                    }
                }
                // 审限前3天：红色预警
                else if (daysUntilDue <= 3 && daysUntilDue > 0) {
                    if (!"RED_WARNING".equals(todo.getPriority())) {
                        todo.setPriority("RED_WARNING");
                        updated = true;
                        log.warn("待办紧急预警（3天内）：ID={}, 标题={}, 截止日期={}",
                                todo.getId(), todo.getTitle(), todo.getDueDate());
                    }
                }
                // 审限当天：红色+推送
                else if (daysUntilDue == 0) {
                    if (!"URGENT".equals(todo.getPriority())) {
                        todo.setPriority("URGENT");
                        updated = true;
                        log.error("待办今日到期：ID={}, 标题={}, 截止日期={}",
                                todo.getId(), todo.getTitle(), todo.getDueDate());
                        // 发送推送通知
                        notificationService.sendTodoOverdueNotification(
                            todo.getId(),
                            todo.getTitle(),
                            todo.getDueDate().toString(),
                            todo.getAssigneeId()
                        );
                    }
                }
                // 逾期：标记为OVERDUE优先级
                else if (daysUntilDue < 0) {
                    if (!"OVERDUE".equals(todo.getPriority())) {
                        todo.setPriority("OVERDUE");
                        updated = true;
                        log.error("待办已逾期：ID={}, 标题={}, 截止日期={}, 逾期天数={}",
                                todo.getId(), todo.getTitle(), todo.getDueDate(), Math.abs(daysUntilDue));
                    }
                }

                if (updated) {
                    todoRepository.save(todo);
                    updatedCount++;
                }
            }

            // 2. 检查案件审限逾期预警
            List<Case> cases = caseRepository.findAll().stream()
                    .filter(c -> !c.getDeleted())
                    .filter(c -> !"closed".equals(c.getStatus()) && !"archived".equals(c.getStatus()))
                    .filter(c -> c.getDeadlineDate() != null)
                    .collect(java.util.stream.Collectors.toList());

            for (Case caseEntity : cases) {
                long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), caseEntity.getDeadlineDate());

                // 审限前7天预警
                if (daysUntilDeadline <= 7 && daysUntilDeadline > 3) {
                    log.info("案件审限预警（7天内）：ID={}, 名称={}, 审限={}",
                            caseEntity.getId(), caseEntity.getCaseName(), caseEntity.getDeadlineDate());
                    // 发送预警通知
                    caseEntity.getOwnerId();
                    notificationService.sendCaseDeadlineNotification(
                        caseEntity.getId(),
                        caseEntity.getCaseName(),
                        caseEntity.getDeadlineDate().toString(),
                        caseEntity.getOwnerId()
                    );
                }
                // 审限前3天红色预警
                else if (daysUntilDeadline <= 3 && daysUntilDeadline > 0) {
                    log.warn("案件审限紧急预警（3天内）：ID={}, 名称={}, 审限={}",
                            caseEntity.getId(), caseEntity.getCaseName(), caseEntity.getDeadlineDate());
                    // 发送紧急通知
                    notificationService.sendCaseDeadlineNotification(
                        caseEntity.getId(),
                        caseEntity.getCaseName(),
                        caseEntity.getDeadlineDate().toString(),
                        caseEntity.getOwnerId()
                    );
                }
                // 审限当天
                else if (daysUntilDeadline == 0) {
                    log.error("案件审限今日到期：ID={}, 名称={}, 审限={}",
                            caseEntity.getId(), caseEntity.getCaseName(), caseEntity.getDeadlineDate());
                    // 发送推送通知
                    notificationService.sendCaseDeadlineNotification(
                        caseEntity.getId(),
                        caseEntity.getCaseName(),
                        caseEntity.getDeadlineDate().toString(),
                        caseEntity.getOwnerId()
                    );
                }
            }

            log.info("逾期预警检查完成，更新了{}条待办记录", updatedCount);

        } catch (Exception e) {
            log.error("逾期预警检查执行失败", e);
        }
    }

    /**
     * 每小时检查一次紧急待办（3天内到期的）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkUrgentTodos() {
        try {
            long urgentCount = todoRepository.findAll().stream()
                    .filter(t -> !t.getDeleted())
                    .filter(t -> "PENDING".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus()))
                    .filter(t -> t.getDueDate() != null)
                    .filter(t -> {
                        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), t.getDueDate());
                        return daysUntilDue <= 3 && daysUntilDue >= 0;
                    })
                    .count();

            if (urgentCount > 0) {
                log.warn("当前有{}个待办将在3天内到期，请及时处理", urgentCount);
                // 发送紧急待办统计通知
                notificationService.sendUrgentTodoCountNotification(urgentCount);
            }
        } catch (Exception e) {
            log.error("检查紧急待办失败", e);
        }
    }
}
