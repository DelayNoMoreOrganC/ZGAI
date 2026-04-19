package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

/**
 * 角色创建请求
 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;

    private List<Long> permissionIds;
}
