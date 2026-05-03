package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "\"role\"")
public class Role extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "角色编码不能为空")
    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column
    private String description;
}
