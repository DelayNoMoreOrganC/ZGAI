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
            "CASE_CREATE", "CASE_VIEW", "CASE_EDIT", "CASE_DELETE", "CASE_ARCHIVE", "CASE_ARCHIVE_REVIEW",
            "CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_EDIT", "CLIENT_DELETE",
            "STATISTICS_VIEW", "STATISTICS_EXPORT",
            "DOCUMENT_VIEW", "DOCUMENT_EDIT", "DOCUMENT_DELETE",
            "CALENDAR_VIEW", "CALENDAR_EDIT", "CALENDAR_DELETE",
            "TODO_VIEW", "TODO_EDIT", "TODO_DELETE",
            "APPROVAL_VIEW", "APPROVAL_EDIT", "APPROVAL_DELETE", "SEAL_APPROVE",
            "FINANCE_VIEW", "FINANCE_EDIT",
            "CLIENT_VIEW_ALL", "CASE_FILING_REVIEW", "CASE_FILING_FINAL_APPROVE",
            "CASE_FILING_MANAGE", "INVOICE_PROCESS",
            "USER_VIEW", "USER_EDIT", "ROLE_VIEW", "ROLE_EDIT",
            "AI_CONFIG", "KNOWLEDGE_MANAGE", "SYSTEM_CONFIG"
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

        return authorityCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public List<String> loadRoleCodes(User user) {
        Set<String> roleCodes = new LinkedHashSet<>();
        userRoleRepository.findByUserId(user.getId()).forEach(userRole ->
                roleRepository.findById(userRole.getRoleId())
                        .filter(role -> !Boolean.TRUE.equals(role.getDeleted()))
                        .map(Role::getRoleCode)
                        .ifPresent(roleCodes::add));
        return List.copyOf(roleCodes);
    }
}
