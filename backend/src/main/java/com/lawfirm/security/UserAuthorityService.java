package com.lawfirm.security;

import com.lawfirm.entity.Role;
import com.lawfirm.entity.User;
import com.lawfirm.repository.PermissionRepository;
import com.lawfirm.repository.RolePermissionRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centralizes account validity and authority resolution for password and JWT authentication.
 */
@Service
@RequiredArgsConstructor
public class UserAuthorityService {

    private static final List<String> ADMIN_AUTHORITIES = Arrays.asList(
            "CASE_CREATE", "CASE_VIEW", "CASE_EDIT", "CASE_DELETE", "CASE_ARCHIVE",
            "CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_EDIT", "CLIENT_DELETE",
            "STATISTICS_VIEW", "STATISTICS_EXPORT",
            "DOCUMENT_VIEW", "DOCUMENT_EDIT", "DOCUMENT_DELETE",
            "CALENDAR_VIEW", "CALENDAR_EDIT", "CALENDAR_DELETE",
            "TODO_VIEW", "TODO_EDIT", "TODO_DELETE",
            "APPROVAL_VIEW", "APPROVAL_EDIT", "APPROVAL_DELETE",
            "FINANCE_VIEW", "FINANCE_EDIT",
            "USER_VIEW", "USER_EDIT", "ROLE_VIEW", "ROLE_EDIT",
            "AI_CONFIG", "SYSTEM_CONFIG"
    );

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public User requireActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .filter(candidate -> !Boolean.TRUE.equals(candidate.getDeleted()))
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已停用");
        }
        return user;
    }

    public List<SimpleGrantedAuthority> loadAuthorities(User user) {
        Set<String> authorityCodes = new LinkedHashSet<>();
        authorityCodes.add("ROLE_USER");

        userRoleRepository.findByUserId(user.getId()).forEach(userRole -> {
            Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
            if (role == null || Boolean.TRUE.equals(role.getDeleted())) {
                return;
            }
            authorityCodes.add("ROLE_" + role.getRoleCode());
            rolePermissionRepository.findByRoleId(role.getId()).forEach(rolePermission ->
                    permissionRepository.findById(rolePermission.getPermissionId())
                            .filter(permission -> !Boolean.TRUE.equals(permission.getDeleted()))
                            .ifPresent(permission -> authorityCodes.add(permission.getPermissionCode())));
            if ("ADMIN".equals(role.getRoleCode())) {
                authorityCodes.addAll(ADMIN_AUTHORITIES);
            }
        });

        // 田颖思在案件结案/归档前可修订立案信息，数据范围仍由 CaseService 校验。
        if ("田颖思".equals(user.getRealName())) {
            authorityCodes.add("CASE_EDIT");
        }

        return authorityCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
