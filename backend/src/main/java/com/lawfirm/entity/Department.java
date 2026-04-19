package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;

/**
 * 部门实体
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "department")
public class Department extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "部门名称不能为空")
    @Column(name = "dept_name", nullable = false, length = 50)
    private String deptName;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "leader_id")
    private Long leaderId;

    @Column
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(Long leaderId) {
        this.leaderId = leaderId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
