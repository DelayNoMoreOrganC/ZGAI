package com.lawfirm.controller;

import com.lawfirm.service.StatisticsService;
import com.lawfirm.service.TodoService;
import com.lawfirm.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作台控制器
 */
@Slf4j
@RestController
@RequestMapping("workbench")
@RequiredArgsConstructor
public class WorkbenchController {

    private final StatisticsService statisticsService;
    private final TodoService todoService;
    private final CalendarService calendarService;

    /**
     * 获取工作台统计数据
     * GET /api/workbench/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
            return statisticsService.getStatsCards(start, end);
        } catch (Exception e) {
            log.error("获取工作台统计数据失败", e);
            throw new RuntimeException("获取工作台统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取案件数量趋势
     * GET /api/workbench/case-trend
     */
    @GetMapping("/case-trend")
    public Map<String, Object> getCaseTrend(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
            return statisticsService.getCaseTrend(period, start, end);
        } catch (Exception e) {
            log.error("获取案件趋势失败", e);
            throw new RuntimeException("获取案件趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取待办统计
     * GET /api/workbench/todo-stats
     */
    @GetMapping("/todo-stats")
    public Map<String, Object> getTodoStats(@RequestParam Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            var todos = todoService.getTodosByAssignee(userId);
            result.put("total", todos.size());
            result.put("pending", todos.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
            result.put("completed", todos.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count());
            result.put("overdue", todos.stream().filter(t -> t.getOverdue() != null && t.getOverdue()).count());
            return result;
        } catch (Exception e) {
            log.error("获取待办统计失败", e);
            throw new RuntimeException("获取待办统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取日程统计
     * GET /api/workbench/calendar-stats
     */
    @GetMapping("/calendar-stats")
    public Map<String, Object> getCalendarStats(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> result = new HashMap<>();
            var calendars = calendarService.getCalendarsByUser(userId);
            result.put("total", calendars.size());
            result.put("upcoming", calendars.stream().filter(c -> c.getStartTime().isAfter(java.time.LocalDateTime.now())).count());
            return result;
        } catch (Exception e) {
            log.error("获取日程统计失败", e);
            throw new RuntimeException("获取日程统计失败: " + e.getMessage());
        }
    }
}
