package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "permission")
public class Permission extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "权限编码不能为空")
    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String permissionCode;

    @NotBlank(message = "权限名称不能为空")
    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    @Column(name = "resource_type", length = 20)
    private String resourceType;

    @Column(name = "resource_url")
    private String resourceUrl;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
