package com.lawfirm.repository;

import com.lawfirm.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限Repository
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 根据权限编码查找权限
     */
    Optional<Permission> findByPermissionCode(String permissionCode);

    /**
     * 根据父权限ID查找子权限列表
     */
    List<Permission> findByParentId(Long parentId);

    /**
     * 根据资源类型查找权限列表
     */
    List<Permission> findByResourceType(String resourceType);
}
