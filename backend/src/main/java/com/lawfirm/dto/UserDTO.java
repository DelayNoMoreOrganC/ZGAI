package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户DTO
 */
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Long departmentId;
    private String departmentName;
    private String position;
    private String avatar;
    private Integer status;
    private String statusDesc;
    private List<String> roles;
    private LocalDateTime lastLoginTime;
}
