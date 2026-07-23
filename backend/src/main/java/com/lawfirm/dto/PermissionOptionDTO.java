package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 可分配权限的最小展示信息。
 */
@Data
@AllArgsConstructor
public class PermissionOptionDTO {
    private Long id;
    private String permissionCode;
    private String permissionName;
}
