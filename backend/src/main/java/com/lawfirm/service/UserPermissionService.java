package com.lawfirm.service;

import com.lawfirm.entity.Permission;
import com.lawfirm.entity.Role;
import com.lawfirm.entity.User;
import com.lawfirm.repository.PermissionRepository;
import com.lawfirm.repository.RolePermissionRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

/** Resolves business permissions without relying on a person's name or the current HTTP session. */
@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public boolean hasPermission(User user, String permissionCode) {
        if (user == null || permissionCode == null || Boolean.TRUE.equals(user.getDeleted())) {
            return false;
        }
        if ("admin".equals(user.getUsername())) {
            return true;
        }
        return userRoleRepository.findByUserId(user.getId()).stream().anyMatch(userRole -> {
            Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
            if (role == null || Boolean.TRUE.equals(role.getDeleted())) {
                return false;
            }
            return rolePermissionRepository.findByRoleId(role.getId()).stream().anyMatch(rolePermission -> {
                Permission permission = permissionRepository.findById(rolePermission.getPermissionId()).orElse(null);
                return permission != null
                        && !Boolean.TRUE.equals(permission.getDeleted())
                        && permissionCode.equals(permission.getPermissionCode());
            });
        });
    }

    public Optional<User> findFirstActiveUserByPermission(String permissionCode) {
        return findFirstActiveUserByPermission(permissionCode, null);
    }

    public Optional<User> findFirstActiveUserByPermission(String permissionCode, String preferredRoleCode) {
        return userRepository.findAll().stream()
                .filter(this::isActive)
                .filter(user -> hasPermission(user, permissionCode))
                .sorted(Comparator
                        .comparing((User user) -> preferredRoleCode == null || hasRole(user, preferredRoleCode) ? 0 : 1)
                        .thenComparing(User::getId))
                .findFirst();
    }

    public boolean hasRole(User user, String roleCode) {
        if (user == null || roleCode == null) {
            return false;
        }
        return userRoleRepository.findByUserId(user.getId()).stream().anyMatch(userRole ->
                roleRepository.findById(userRole.getRoleId())
                        .filter(role -> !Boolean.TRUE.equals(role.getDeleted()))
                        .map(role -> roleCode.equals(role.getRoleCode()))
                        .orElse(false));
    }

    private boolean isActive(User user) {
        return user != null
                && !Boolean.TRUE.equals(user.getDeleted())
                && (user.getStatus() == null || user.getStatus() == 1);
    }
}
