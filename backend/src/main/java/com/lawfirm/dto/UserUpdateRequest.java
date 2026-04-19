package com.lawfirm.dto;

import javax.validation.constraints.Email;
import lombok.Data;

/**
 * 用户更新请求
 */
@Data
public class UserUpdateRequest {
    private String realName;
    private String email;
    private String phone;
    private Long departmentId;
    private String position;
    private String avatar;
    private Integer status;
}
