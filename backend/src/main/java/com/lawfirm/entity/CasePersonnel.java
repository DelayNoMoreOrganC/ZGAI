package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;

/**
 * 案件承办人员
 */
@Data
@Entity
@Table(name = "case_personnel")
public class CasePersonnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联案件ID
     */
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    /**
     * 姓名
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 职位（法官、书记员、执行员等）
     */
    @Column(length = 50)
    private String position;

    /**
     * 电话
     */
    @Column(length = 20)
    private String phone;

    /**
     * 法院
     */
    @Column(length = 200)
    private String court;

    /**
     * 部门
     */
    @Column(length = 100)
    private String department;

    /**
     * 备注
     */
    @Column(length = 500)
    private String remark;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
