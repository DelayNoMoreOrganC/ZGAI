package com.lawfirm.service;

import com.lawfirm.dto.UserCreateRequest;
import com.lawfirm.dto.UserDTO;
import com.lawfirm.dto.UserOptionDTO;
import com.lawfirm.dto.UserUpdateRequest;
import com.lawfirm.entity.User;
import com.lawfirm.entity.UserRole;
import com.lawfirm.exception.DuplicateResourceException;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建用户
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDTO createUser(UserCreateRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("用户", "username", request.getUsername());
        }

        User user = new User();
        BeanUtils.copyProperties(request, user);

        // 加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMustChangePassword(!isProtectedDeveloperAccount(user.getUsername()));

        user = userRepository.save(user);

        if (request.getRoleIds() != null) {
            assignRoles(user.getId(), request.getRoleIds());
        }

        return toDTO(user);
    }

    /**
     * 更新用户
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        requireMutableAccount(user);

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDepartmentId() != null) {
            user.setDepartmentId(request.getDepartmentId());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        user = userRepository.save(user);

        if (request.getRoleIds() != null) {
            assignRoles(user.getId(), request.getRoleIds());
        }

        return toDTO(user);
    }

    /**
     * 删除用户（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        requireMutableAccount(user);

        user.setDeleted(true);
        userRepository.save(user);
    }

    /**
     * 获取用户列表
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> getUserList(int page, int size, String keyword, Long departmentId, Integer status) {
        int safeSize = Math.max(1, Math.min(size, 300));
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize,
                Sort.by(Sort.Direction.ASC, "realName").and(Sort.by(Sort.Direction.ASC, "id")));
        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;

        Specification<User> specification = (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (normalizedKeyword != null) {
                String likeKeyword = "%" + normalizedKeyword + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), likeKeyword),
                        cb.like(cb.lower(root.get("realName")), likeKeyword)
                ));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("departmentId"), departmentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(specification, pageable);

        return userPage.map(this::toDTO);
    }

    /**
     * 获取用户详情
     */
    @Transactional(readOnly = true)
    public UserDTO getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        return toDTO(user);
    }

    /**
     * 返回业务下拉所需的最小在职员工目录，不包含账号、电话、邮箱和角色。
     */
    @Transactional(readOnly = true)
    public List<UserOptionDTO> getUserOptions(String keyword, Long departmentId, int size) {
        int safeSize = Math.max(1, Math.min(size, 300));
        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;
        Specification<User> specification = (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            predicates.add(cb.equal(root.get("status"), 1));
            if (normalizedKeyword != null) {
                predicates.add(cb.like(cb.lower(root.get("realName")), "%" + normalizedKeyword + "%"));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("departmentId"), departmentId));
            }
            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };

        List<User> users = userRepository.findAll(specification,
                PageRequest.of(0, safeSize, Sort.by(Sort.Direction.ASC, "realName")
                        .and(Sort.by(Sort.Direction.ASC, "id"))))
                .getContent();
        List<Long> departmentIds = users.stream()
                .map(User::getDepartmentId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> departmentNames = departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(com.lawfirm.entity.Department::getId,
                        com.lawfirm.entity.Department::getDeptName));

        return users.stream()
                .map(user -> new UserOptionDTO(
                        user.getId(),
                        user.getRealName(),
                        user.getDepartmentId(),
                        departmentNames.get(user.getDepartmentId()),
                        user.getPosition()))
                .collect(Collectors.toList());
    }

    /**
     * 启用/禁用用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new InvalidParameterException("status", "用户状态只能是启用或禁用");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        requireMutableAccount(user);

        user.setStatus(status);
        userRepository.save(user);
    }

    /**
     * 重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new InvalidParameterException("newPassword", "新密码不能为空");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(!isProtectedDeveloperAccount(user.getUsername()));
        userRepository.save(user);
    }

    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new InvalidParameterException("oldPassword", "旧密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new InvalidParameterException("newPassword", "新密码不能为空");
        }
        if (newPassword.length() < 8 || newPassword.length() > 100) {
            throw new InvalidParameterException("newPassword", "新密码长度必须为8至100位");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidParameterException("oldPassword", "原密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new InvalidParameterException("newPassword", "新密码不能与当前密码相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    /**
     * 分配角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        requireMutableAccount(user);

        List<Long> normalizedRoleIds = roleIds == null
                ? java.util.Collections.emptyList()
                : roleIds.stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        long validRoleCount = roleRepository.findAllById(normalizedRoleIds).stream()
                .filter(role -> !Boolean.TRUE.equals(role.getDeleted()))
                .count();
        if (validRoleCount != normalizedRoleIds.size()) {
            throw new InvalidParameterException("roleIds", "角色列表包含不存在或已停用的角色");
        }

        // 删除现有角色
        userRoleRepository.deleteByUserId(userId);

        // 分配新角色
        if (!normalizedRoleIds.isEmpty()) {
            List<UserRole> userRoles = normalizedRoleIds.stream()
                    .map(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .collect(Collectors.toList());

            userRoleRepository.saveAll(userRoles);
        }
    }

    /**
     * 更新最后登录时间
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginTime(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    // 辅助方法

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);

        dto.setStatusDesc(user.getStatus() == 1 ? "启用" : "禁用");

        // 设置部门名称
        if (user.getDepartmentId() != null) {
            departmentRepository.findById(user.getDepartmentId())
                    .ifPresent(dept -> dto.setDepartmentName(dept.getDeptName()));
        }

        // 设置角色列表
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        dto.setRoleIds(userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList()));
        List<String> roles = userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId())
                        .map(role -> role.getRoleName())
                        .orElse("未知角色"))
                .collect(Collectors.toList());
        dto.setRoles(roles);

        return dto;
    }

    private void requireMutableAccount(User user) {
        String username = user.getUsername();
        if ("admin".equalsIgnoreCase(username) || "amin".equalsIgnoreCase(username)) {
            throw new InvalidParameterException("userId", "受保护开发管理员账号不能通过员工管理修改");
        }
    }

    private boolean isProtectedDeveloperAccount(String username) {
        return "admin".equalsIgnoreCase(username) || "amin".equalsIgnoreCase(username);
    }
}
