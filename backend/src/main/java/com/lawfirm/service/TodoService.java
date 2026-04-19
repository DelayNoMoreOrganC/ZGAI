package com.lawfirm.service;

import com.lawfirm.dto.TodoDTO;
import com.lawfirm.entity.Todo;
import com.lawfirm.repository.TodoRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 待办管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

    /**
     * 创建待办
     */
    @Transactional
    public TodoDTO createTodo(TodoDTO dto, Long userId) {
        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        Todo todo = new Todo();
        todo.setTitle(dto.getTitle());
        todo.setDescription(dto.getDescription());
        todo.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");
        todo.setPriority(dto.getPriority() != null ? dto.getPriority() : "NORMAL");
        todo.setDueDate(dto.getDueDate());
        todo.setAssigneeId(dto.getAssigneeId());
        todo.setCaseId(dto.getCaseId());
        todo.setReminder(dto.getReminder() != null ? dto.getReminder() : false);

        todo = todoRepository.save(todo);
        log.info("创建待办成功: {}", todo.getId());

        return convertToDTO(todo);
    }

    /**
     * 更新待办
     */
    @Transactional
    public TodoDTO updateTodo(Long id, TodoDTO dto) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("待办不存在"));

        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        todo.setTitle(dto.getTitle());
        todo.setDescription(dto.getDescription());
        todo.setStatus(dto.getStatus());
        todo.setPriority(dto.getPriority());
        todo.setDueDate(dto.getDueDate());
        todo.setAssigneeId(dto.getAssigneeId());
        todo.setCaseId(dto.getCaseId());
        todo.setReminder(dto.getReminder());

        // 如果状态变更为已完成，记录完成时间
        if ("COMPLETED".equals(dto.getStatus()) && todo.getCompletedAt() == null) {
            todo.setCompletedAt(LocalDateTime.now());
        } else if (!"COMPLETED".equals(dto.getStatus())) {
            todo.setCompletedAt(null);
        }

        todo = todoRepository.save(todo);
        log.info("更新待办成功: {}", id);

        return convertToDTO(todo);
    }

    /**
     * 删除待办
     */
    @Transactional
    public void deleteTodo(Long id) {
        if (!todoRepository.existsById(id)) {
            throw new IllegalArgumentException("待办不存在");
        }
        todoRepository.deleteById(id);
        log.info("删除待办成功: {}", id);
    }

    /**
     * 根据ID查询待办
     */
    public TodoDTO getTodoById(Long id) {
        return todoRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("待办不存在"));
    }

    /**
     * 查询用户的待办
     */
    public List<TodoDTO> getTodosByAssignee(Long assigneeId) {
        return todoRepository.findByAssigneeId(assigneeId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户的待办（按优先级排序）
     */
    public List<TodoDTO> getTodosByAssigneeWithPrioritySort(Long assigneeId) {
        return todoRepository.findByAssigneeId(assigneeId).stream()
                .sorted((a, b) -> {
                    // 首先按状态排序：逾期 > 进行中 > 待处理 > 已完成
                    int statusCompare = compareStatus(a.getStatus(), b.getStatus());
                    if (statusCompare != 0) return statusCompare;

                    // 然后按优先级排序：紧急 > 重要 > 普通
                    int priorityCompare = comparePriority(a.getPriority(), b.getPriority());
                    if (priorityCompare != 0) return priorityCompare;

                    // 最后按截止时间排序
                    if (a.getDueDate() == null) return 1;
                    if (b.getDueDate() == null) return -1;
                    return a.getDueDate().compareTo(b.getDueDate());
                })
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询待办的待办
     */
    public List<TodoDTO> getPendingTodos(Long assigneeId) {
        return todoRepository.findByAssigneeId(assigneeId).stream()
                .filter(t -> "PENDING".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询已完成的待办
     */
    public List<TodoDTO> getCompletedTodos(Long assigneeId) {
        return todoRepository.findByAssigneeId(assigneeId).stream()
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询逾期的待办
     */
    public List<TodoDTO> getOverdueTodos(Long assigneeId) {
        LocalDateTime now = LocalDateTime.now();
        return todoRepository.findByAssigneeId(assigneeId).stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now)
                        && !"COMPLETED".equals(t.getStatus()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询案件的待办（使用数据库查询优化）
     */
    public List<TodoDTO> getTodosByCase(Long caseId) {
        // 使用数据库查询，而不是findAll().stream()
        return todoRepository.findByCaseIdOrderByDueDateAsc(caseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询待办（性能优化 - 使用Spring Data分页）
     */
    public com.lawfirm.util.PageResult<TodoDTO> getTodos(int page, int size, Long assigneeId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 如果没有指定assigneeId，返回空结果
        if (assigneeId == null) {
            return new com.lawfirm.util.PageResult<>((long) page, (long) size, 0L, List.of());
        }

        // 使用Spring Data分页查询，避免全表加载
        org.springframework.data.domain.Page<Todo> todoPage = todoRepository.findByAssigneeId(assigneeId, pageable);

        List<TodoDTO> dtoList = todoPage.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new com.lawfirm.util.PageResult<>((long) page, (long) size, todoPage.getTotalElements(), dtoList);
    }

    /**
     * 状态排序比较
     */
    private int compareStatus(String status1, String status2) {
        int order1 = getStatusOrder(status1);
        int order2 = getStatusOrder(status2);
        return Integer.compare(order2, order1); // 降序
    }

    private int getStatusOrder(String status) {
        if (status == null) return 0;
        switch (status) {
            case "OVERDUE":
                return 4;
            case "IN_PROGRESS":
                return 3;
            case "PENDING":
                return 2;
            case "COMPLETED":
                return 1;
            default:
                return 0;
        }
    }

    /**
     * 优先级排序比较
     */
    private int comparePriority(String priority1, String priority2) {
        int order1 = getPriorityOrder(priority1);
        int order2 = getPriorityOrder(priority2);
        return Integer.compare(order2, order1); // 降序
    }

    private int getPriorityOrder(String priority) {
        if (priority == null) return 0;
        switch (priority) {
            case "URGENT":
                return 3;
            case "IMPORTANT":
                return 2;
            case "NORMAL":
                return 1;
            default:
                return 0;
        }
    }

    /**
     * 转换为DTO
     */
    private TodoDTO convertToDTO(Todo todo) {
        TodoDTO dto = new TodoDTO();
        dto.setId(todo.getId());
        dto.setTitle(todo.getTitle());
        dto.setDescription(todo.getDescription());
        dto.setStatus(todo.getStatus());
        dto.setPriority(todo.getPriority());
        dto.setDueDate(todo.getDueDate());
        dto.setAssigneeId(todo.getAssigneeId());
        dto.setCaseId(todo.getCaseId());
        dto.setReminder(todo.getReminder());
        dto.setCompletedAt(todo.getCompletedAt());
        dto.setCreatedAt(todo.getCreatedAt());
        dto.setUpdatedAt(todo.getUpdatedAt());

        // 加载负责人名称
        userRepository.findById(todo.getAssigneeId()).ifPresent(u -> dto.setAssigneeName(u.getRealName()));

        // 加载案件名称
        if (todo.getCaseId() != null) {
            caseRepository.findById(todo.getCaseId()).ifPresent(c -> dto.setCaseName(c.getCaseName()));
        }

        // 计算是否逾期
        if (todo.getDueDate() != null && !"COMPLETED".equals(todo.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            dto.setOverdue(todo.getDueDate().isBefore(now));

            // 计算剩余天数
            long days = ChronoUnit.DAYS.between(now, todo.getDueDate());
            dto.setRemainingDays(days);
        } else {
            dto.setOverdue(false);
        }

        return dto;
    }

    /**
     * 按紧急程度排序（逾期优先，然后按截止时间）
     */
    public List<TodoDTO> sortByUrgency(List<TodoDTO> todos) {
        return todos.stream()
                .sorted((a, b) -> {
                    // 首先逾期优先
                    if (a.getOverdue() && !b.getOverdue()) return -1;
                    if (!a.getOverdue() && b.getOverdue()) return 1;

                    // 都逾期或都不逾期，按剩余天数排序
                    if (a.getRemainingDays() == null) return 1;
                    if (b.getRemainingDays() == null) return -1;
                    return Long.compare(a.getRemainingDays(), b.getRemainingDays());
                })
                .collect(Collectors.toList());
    }

    /**
     * 按优先级排序
     */
    public List<TodoDTO> sortByPriority(List<TodoDTO> todos) {
        return todos.stream()
                .sorted((a, b) -> {
                    // 使用已定义的优先级比较方法
                    return comparePriority(a.getPriority(), b.getPriority());
                })
                .collect(Collectors.toList());
    }
}
