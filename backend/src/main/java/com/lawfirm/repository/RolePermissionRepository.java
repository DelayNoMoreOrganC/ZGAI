package com.lawfirm.repository;

import com.lawfirm.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色权限关联Repository
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * 根据角色ID查找权限关联
     */
    List<RolePermission> findByRoleId(Long roleId);

    /**
     * 根据权限ID查找角色关联
     */
    List<RolePermission> findByPermissionId(Long permissionId);

    /**
     * 删除角色的所有权限
     */
    void deleteByRoleId(Long roleId);
}
