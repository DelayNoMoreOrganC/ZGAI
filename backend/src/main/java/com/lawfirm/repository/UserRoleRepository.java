package com.lawfirm.repository;

import com.lawfirm.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关联Repository
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * 根据用户ID查找角色关联
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * 根据角色ID查找用户关联
     */
    List<UserRole> findByRoleId(Long roleId);

    /**
     * 删除用户的所有角色
     */
    void deleteByUserId(Long userId);
}
