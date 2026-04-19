package com.lawfirm.controller;

import com.lawfirm.dto.TodoDTO;
import com.lawfirm.service.TodoService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import java.util.List;

/**
 * 待办管理控制器
 */
@Slf4j
@RestController
@RequestMapping("todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    private final SecurityUtils securityUtils;

    /**
     * 创建待办
     * POST /api/todos
     */
    @PostMapping
    public Result<TodoDTO> createTodo(@Valid @RequestBody TodoDTO dto) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            TodoDTO result = todoService.createTodo(dto, userId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("创建待办失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建待办异常", e);
            return Result.error("创建待办失败");
        }
    }

    /**
     * 更新待办
     * PUT /api/todos/{id}
     */
    @PutMapping("/{id}")
    public Result<TodoDTO> updateTodo(@PathVariable Long id, @Valid @RequestBody TodoDTO dto) {
        try {
            TodoDTO result = todoService.updateTodo(id, dto);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("更新待办失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新待办异常", e);
            return Result.error("更新待办失败");
        }
    }

    /**
     * 删除待办
     * DELETE /api/todos/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTodo(@PathVariable Long id) {
        try {
            todoService.deleteTodo(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除待办失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除待办异常", e);
            return Result.error("删除待办失败");
        }
    }

    /**
     * 查询待办详情
     * GET /api/todos/{id}
     */
    @GetMapping("/{id}")
    public Result<TodoDTO> getTodo(@PathVariable Long id) {
        try {
            TodoDTO result = todoService.getTodoById(id);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("查询待办失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询待办异常", e);
            return Result.error("查询待办失败");
        }
    }

    /**
     * 查询用户的待办
     * GET /api/todos/assignee/{assigneeId}
     */
    @GetMapping("/assignee/{assigneeId}")
    public Result<List<TodoDTO>> getTodosByAssignee(@PathVariable Long assigneeId) {
        try {
            List<TodoDTO> result = todoService.getTodosByAssignee(assigneeId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询用户待办异常", e);
            return Result.error("查询用户待办失败");
        }
    }

    /**
     * 查询用户的待办（按优先级排序）
     * GET /api/todos/assignee/{assigneeId}/priority
     */
    @GetMapping("/assignee/{assigneeId}/priority")
    public Result<List<TodoDTO>> getTodosByAssigneeWithPrioritySort(@PathVariable Long assigneeId) {
        try {
            List<TodoDTO> result = todoService.getTodosByAssigneeWithPrioritySort(assigneeId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询用户待办（按优先级排序）异常", e);
            return Result.error("查询待办失败");
        }
    }

    /**
     * 查询待办的待办
     * GET /api/todos/assignee/{assigneeId}/pending
     */
    @GetMapping("/assignee/{assigneeId}/pending")
    public Result<List<TodoDTO>> getPendingTodos(@PathVariable Long assigneeId) {
        try {
            List<TodoDTO> result = todoService.getPendingTodos(assigneeId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询待办的待办异常", e);
            return Result.error("查询待办失败");
        }
    }

    /**
     * 查询已完成的待办
     * GET /api/todos/assignee/{assigneeId}/completed
     */
    @GetMapping("/assignee/{assigneeId}/completed")
    public Result<List<TodoDTO>> getCompletedTodos(@PathVariable Long assigneeId) {
        try {
            List<TodoDTO> result = todoService.getCompletedTodos(assigneeId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询已完成待办异常", e);
            return Result.error("查询待办失败");
        }
    }

    /**
     * 查询逾期的待办
     * GET /api/todos/assignee/{assigneeId}/overdue
     */
    @GetMapping("/assignee/{assigneeId}/overdue")
    public Result<List<TodoDTO>> getOverdueTodos(@PathVariable Long assigneeId) {
        try {
            List<TodoDTO> result = todoService.getOverdueTodos(assigneeId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询逾期待办异常", e);
            return Result.error("查询待办失败");
        }
    }

    /**
     * 查询案件的待办
     * GET /api/todos/case/{caseId}
     */
    @GetMapping("/case/{caseId}")
    public Result<List<TodoDTO>> getTodosByCase(@PathVariable Long caseId) {
        try {
            List<TodoDTO> result = todoService.getTodosByCase(caseId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询案件待办异常", e);
            return Result.error("查询案件待办失败");
        }
    }

    /**
     * 按条件查询待办（PRD要求的格式）
     * GET /api/todos/search?assignee={userId}&status=pending&sort=urgency
     * 必须在 /{id} 之前定义，否则会被 /{id} 捕获
     */
    @GetMapping("/search")
    public Result<List<TodoDTO>> getTodosByFilter(
            @RequestParam Long assignee,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort) {
        try {
            List<TodoDTO> result;

            // 根据状态筛选
            if ("pending".equals(status)) {
                result = todoService.getPendingTodos(assignee);
            } else if ("completed".equals(status)) {
                result = todoService.getCompletedTodos(assignee);
            } else if ("overdue".equals(status)) {
                result = todoService.getOverdueTodos(assignee);
            } else {
                result = todoService.getTodosByAssignee(assignee);
            }

            // 根据排序要求排序
            if ("urgency".equals(sort)) {
                // 按紧急程度排序（逾期优先，然后按截止时间）
                result = todoService.sortByUrgency(result);
            } else if ("priority".equals(sort)) {
                // 按优先级排序
                result = todoService.sortByPriority(result);
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("按条件查询待办异常", e);
            return Result.error("查询待办失败");
        }
    }

    /**
     * 分页查询待办
     * GET /api/todos?page={page}&size={size}&assigneeId={assigneeId}
     */
    @GetMapping
    public Result<com.lawfirm.util.PageResult<TodoDTO>> getTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long assigneeId) {
        try {
            var result = todoService.getTodos(page, size, assigneeId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询待办异常", e);
            return Result.error("分页查询待办失败");
        }
    }
}
