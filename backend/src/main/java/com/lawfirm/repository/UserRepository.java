package com.lawfirm.repository;

import com.lawfirm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据用户名查找未删除用户
     */
    Optional<User> findByUsernameAndDeletedFalse(String username);

    /**
     * 查询未删除用户分页
     */
    Page<User> findByDeletedFalse(Pageable pageable);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据部门ID查找用户列表
     */
    List<User> findByDepartmentId(Long departmentId);

    /**
     * 根据状态查找用户列表
     */
    List<User> findByStatus(Integer status);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据职位查找用户列表（性能优化）
     */
    List<User> findByPosition(String position);

    @Query("select u.id from User u where u.deleted = false and "
            + "(lower(u.realName) like lower(concat('%', :keyword, '%')) "
            + "or lower(u.username) like lower(concat('%', :keyword, '%')))")
    List<Long> findActiveIdsByNameOrUsername(@Param("keyword") String keyword);
}
