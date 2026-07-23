package com.lawfirm.security;

import com.lawfirm.entity.Permission;
import com.lawfirm.entity.Role;
import com.lawfirm.entity.RolePermission;
import com.lawfirm.entity.User;
import com.lawfirm.entity.UserRole;
import com.lawfirm.repository.PermissionRepository;
import com.lawfirm.repository.RolePermissionRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAuthorityServiceTest {

    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RoleRepository roleRepository;
    private RolePermissionRepository rolePermissionRepository;
    private PermissionRepository permissionRepository;
    private UserAuthorityService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        roleRepository = mock(RoleRepository.class);
        rolePermissionRepository = mock(RolePermissionRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        service = new UserAuthorityService(
                userRepository,
                userRoleRepository,
                roleRepository,
                rolePermissionRepository,
                permissionRepository);
    }

    @Test
    void adminRoleReceivesFullCoreAuthorities() {
        User admin = activeUser(1L, "admin", "开发管理员");
        UserRole userRole = new UserRole();
        userRole.setRoleId(10L);
        Role role = new Role();
        role.setId(10L);
        role.setRoleCode("ADMIN");
        role.setRoleName("管理员");
        role.setDeleted(false);

        when(userRoleRepository.findByUserId(1L)).thenReturn(Collections.singletonList(userRole));
        when(roleRepository.findById(10L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRoleId(10L)).thenReturn(Collections.emptyList());

        List<String> authorities = codes(service.loadAuthorities(admin));

        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("CASE_DELETE"));
        assertTrue(authorities.contains("CLIENT_DELETE"));
        assertTrue(authorities.contains("USER_EDIT"));
        assertTrue(authorities.contains("ROLE_EDIT"));
        assertTrue(authorities.contains("KNOWLEDGE_MANAGE"));
        assertTrue(authorities.contains("SYSTEM_CONFIG"));
        assertTrue(authorities.contains("SEAL_APPROVE"));
    }

    @Test
    void ordinaryLawyerOnlyReceivesAssignedPermissions() {
        User lawyer = activeUser(2L, "张律师", "张律师");
        UserRole userRole = new UserRole();
        userRole.setRoleId(20L);
        Role role = new Role();
        role.setId(20L);
        role.setRoleCode("LAWYER");
        role.setRoleName("律师");
        role.setDeleted(false);
        RolePermission rolePermission = new RolePermission();
        rolePermission.setPermissionId(30L);
        Permission permission = new Permission();
        permission.setPermissionCode("CASE_VIEW");
        permission.setPermissionName("案件查看");
        permission.setDeleted(false);

        when(userRoleRepository.findByUserId(2L)).thenReturn(Collections.singletonList(userRole));
        when(roleRepository.findById(20L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRoleId(20L)).thenReturn(Collections.singletonList(rolePermission));
        when(permissionRepository.findById(30L)).thenReturn(Optional.of(permission));

        List<String> authorities = codes(service.loadAuthorities(lawyer));

        assertTrue(authorities.contains("CASE_VIEW"));
        assertFalse(authorities.contains("CASE_DELETE"));
        assertFalse(authorities.contains("USER_EDIT"));
        assertFalse(authorities.contains("ROLE_EDIT"));
        assertFalse(authorities.contains("KNOWLEDGE_MANAGE"));
        assertFalse(authorities.contains("SYSTEM_CONFIG"));
    }

    @Test
    void aPersonsNameDoesNotGrantHiddenCaseEditPermission() {
        User filingAdmin = activeUser(3L, "田颖思", "田颖思");
        when(userRoleRepository.findByUserId(3L)).thenReturn(Collections.emptyList());

        List<String> authorities = codes(service.loadAuthorities(filingAdmin));

        assertFalse(authorities.contains("CASE_EDIT"));
        assertFalse(authorities.contains("CLIENT_DELETE"));
    }

    @Test
    void deletedOrDisabledUsersCannotReuseJwtAuthentication() {
        User deleted = activeUser(4L, "已删除", "已删除");
        deleted.setDeleted(true);
        when(userRepository.findById(4L)).thenReturn(Optional.of(deleted));

        User disabled = activeUser(5L, "已停用", "已停用");
        disabled.setStatus(0);
        when(userRepository.findById(5L)).thenReturn(Optional.of(disabled));

        assertThrows(UsernameNotFoundException.class, () -> service.requireActiveUser(4L));
        assertThrows(UsernameNotFoundException.class, () -> service.requireActiveUser(5L));
    }

    private User activeUser(Long id, String username, String realName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setPassword("encoded");
        user.setStatus(1);
        user.setDeleted(false);
        return user;
    }

    private List<String> codes(List<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
    }
}
