package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 案件团队成员实体
 */
@Data
@Entity
@Table(name = "case_member", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"case_id", "user_id"})
})
public class CaseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "成员类型不能为空")
    @Column(name = "member_type", nullable = false, length = 20)
    private String memberType;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();
}
