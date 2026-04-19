package com.lawfirm.service;

import com.lawfirm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台数据服务 - 完整版本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TodoRepository todoRepository;
    private final CaseRepository caseRepository;
    private final CalendarRepository calendarRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 获取工作台统计数据
     */
    public Map<String, Object> getDashboardStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 计算本月时间范围
            LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).withSecond(59);

            // 1. 本月新建案件数
            List<com.lawfirm.entity.Case> monthlyCases = caseRepository.findByCreatedAtBetweenAndDeletedFalseOrderByCreatedAtAsc(monthStart, monthEnd);
            stats.put("monthlyCases", (long) monthlyCases.size());

            // 2. 进行中案件数（status='active'或'审理中'）
            Page<com.lawfirm.entity.Case> activeCasesPage = caseRepository.findByStatusAndDeletedFalse("active", PageRequest.of(0, 1));
            long activeCasesCount = activeCasesPage.getTotalElements();
            // 如果active状态没有数据，尝试其他可能的状态值
            if (activeCasesCount == 0) {
                Page<com.lawfirm.entity.Case> processingCases = caseRepository.findByStatusAndDeletedFalse("审理中", PageRequest.of(0, 1));
                activeCasesCount = processingCases.getTotalElements();
            }
            stats.put("activeCases", activeCasesCount);

            // 3. 本月开庭数（calendarType='HEARING'且在本月）
            List<com.lawfirm.entity.Calendar> monthlyHearings = calendarRepository.findByDeletedFalseAndCalendarTypeAndStartTimeBetween(
                "HEARING", monthStart, monthEnd
            );
            stats.put("monthlyHearings", (long) monthlyHearings.size());

            // 4. 待办数（未删除且未完成）
            long pendingTodosCount = todoRepository.countByDeletedFalseAndStatusNotCompleted();
            stats.put("pendingTodos", pendingTodosCount);

            // 5. 本月收费（本月paymentDate的收款总额）
            List<com.lawfirm.entity.Payment> monthlyPayments = paymentRepository.findByPaymentDateBetween(
                monthStart.toLocalDate(), monthEnd.toLocalDate()
            );
            double monthlyIncome = monthlyPayments.stream()
                .mapToDouble(p -> p.getPaymentAmount() != null ? p.getPaymentAmount().doubleValue() : 0.0)
                .sum();
            stats.put("monthlyIncome", monthlyIncome);

            log.info("获取工作台统计数据成功: userId={}, stats={}", userId, stats);

        } catch (Exception e) {
            log.error("获取工作台统计数据失败: userId={}", userId, e);
            stats.put("monthlyCases", 0L);
            stats.put("activeCases", 0L);
            stats.put("monthlyHearings", 0L);
            stats.put("pendingTodos", 0L);
            stats.put("monthlyIncome", 0.0);
        }

        return stats;
    }

    /**
     * 获取用户工作台详情 - 极简版本
     */
    public Map<String, Object> getUserDashboard(Long userId) {
        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("stats", getDashboardStats(userId));
        dashboard.put("recentTodos", java.util.Collections.emptyList());
        dashboard.put("upcomingCalendars", java.util.Collections.emptyList());
        dashboard.put("myActiveCases", java.util.Collections.emptyList());
        dashboard.put("overdueTodoCount", 0L);
        dashboard.put("urgentTodoCount", 0L);

        return dashboard;
    }
}