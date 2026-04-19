package com.lawfirm.repository;

import com.lawfirm.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待办Repository
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    /**
     * 根据负责人ID查找待办列表
     */
    List<Todo> findByAssigneeId(Long assigneeId);

    /**
     * 根据负责人ID分页查找待办列表（性能优化）
     */
    Page<Todo> findByAssigneeId(Long assigneeId, Pageable pageable);

    /**
     * 根据负责人ID查找待办列表（按截止时间排序）
     */
    List<Todo> findByAssigneeIdOrderByDueDateAsc(Long assigneeId);

    /**
     * 根据状态查找待办列表
     */
    List<Todo> findByStatus(String status);

    /**
     * 根据优先级查找待办列表
     */
    List<Todo> findByPriority(String priority);

    /**
     * 查找逾期待办
     */
    @Query("SELECT t FROM Todo t WHERE t.dueDate < :now AND t.status != 'COMPLETED'")
    List<Todo> findOverdueTodos(@Param("now") LocalDateTime now);

    /**
     * 根据案件ID查找待办列表
     */
    List<Todo> findByCaseId(Long caseId);

    /**
     * 根据案件ID查找待办列表（按截止时间排序）
     */
    List<Todo> findByCaseIdOrderByDueDateAsc(Long caseId);

    /**
     * 统计未完成且未删除的待办数量（性能优化）
     */
    @Query("SELECT COUNT(t) FROM Todo t WHERE t.deleted = false AND t.status <> 'COMPLETED'")
    long countByDeletedFalseAndStatusNotCompleted();
}
