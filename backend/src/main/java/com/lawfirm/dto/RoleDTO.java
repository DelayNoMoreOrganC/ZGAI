package com.lawfirm.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色DTO
 */
@Data
public class RoleDTO {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private List<String> permissions;
}
