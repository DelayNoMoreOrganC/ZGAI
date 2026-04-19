package com.lawfirm.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 案件阶段待办事项模板
 */
@Data
@Entity
@Table(name = "stage_todo_template")
public class StageTodoTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 案件阶段名称
     */
    @Column(name = "stage_name", nullable = false)
    private String stageName;

    /**
     * 案件类型（民事/刑事/行政/商事仲裁/非诉）
     */
    @Column(name = "case_type", nullable = false)
    private String caseType;

    /**
     * 待办事项标题
     */
    @Column(name = "todo_title", nullable = false)
    private String todoTitle;

    /**
     * 待办事项描述
     */
    @Column(name = "todo_description", length = 1000)
    private String todoDescription;

    /**
     * 优先级（1-高 2-中 3-低）
     */
    @Column(name = "priority", nullable = false)
    private Integer priority;

    /**
     * 相对天数（相对于阶段开始日的天数，0表示当天，1表示次日，-1表示前一日）
     */
    @Column(name = "relative_days", nullable = false)
    private Integer relativeDays;

    /**
     * 排序序号
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 是否启用
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 是否删除
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isDeleted = false;
        if (isEnabled == null) {
            isEnabled = true;
        }
        if (priority == null) {
            priority = 2; // 默认中等优先级
        }
        if (relativeDays == null) {
            relativeDays = 0; // 默认当天
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
