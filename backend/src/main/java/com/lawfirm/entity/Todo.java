package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * 待办实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "todo", indexes = {
    @Index(name = "idx_todo_assignee", columnList = "assignee_id"),
    @Index(name = "idx_todo_status", columnList = "status"),
    @Index(name = "idx_todo_priority", columnList = "priority"),
    @Index(name = "idx_todo_due_date", columnList = "due_date"),
    @Index(name = "idx_todo_case", columnList = "case_id"),
    @Index(name = "idx_todo_deleted", columnList = "deleted")
})
public class Todo extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "待办标题不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    private String description;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(length = 20)
    private String priority = "NORMAL";

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @NotNull(message = "负责人不能为空")
    @Column(name = "assignee_id", nullable = false)
    private Long assigneeId;

    @Column(name = "case_id")
    private Long caseId;

    @Column
    private Boolean reminder = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
