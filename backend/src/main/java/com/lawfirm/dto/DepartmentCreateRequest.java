package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;

/**
 * 部门创建请求
 */
public class DepartmentCreateRequest {

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Long parentId = 0L;

    private Long leaderId;

    private String description;

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
