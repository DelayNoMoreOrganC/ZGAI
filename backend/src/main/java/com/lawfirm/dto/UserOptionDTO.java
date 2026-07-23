package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 业务表单使用的最小员工目录，避免暴露账号、联系方式和角色信息。
 */
@Data
@AllArgsConstructor
public class UserOptionDTO {
    private Long id;
    private String realName;
    private Long departmentId;
    private String departmentName;
    private String position;
}
