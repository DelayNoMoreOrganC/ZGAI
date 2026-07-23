package com.lawfirm.service;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPermissionServiceTest {

    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RoleRepository roleRepository;
    private RolePermissionRepository rolePermissionRepository;
    private PermissionRepository permissionRepository;
    private UserPermissionService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        roleRepository = mock(RoleRepository.class);
        rolePermissionRepository = mock(RolePermissionRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        service = new UserPermissionService(userRepository, userRoleRepository, roleRepository,
                rolePermissionRepository, permissionRepository);
    }

    @Test
    void resolvesPermissionThroughRoleAssignments() {
        User user = user(10L, "任意财务人员");
        UserRole userRole = new UserRole();
        userRole.setRoleId(20L);
        Role role = new Role();
        role.setId(20L);
        role.setDeleted(false);
        RolePermission rolePermission = new RolePermission();
        rolePermission.setPermissionId(30L);
        Permission permission = new Permission();
        permission.setPermissionCode("INVOICE_PROCESS");
        permission.setDeleted(false);

        when(userRoleRepository.findByUserId(10L)).thenReturn(List.of(userRole));
        when(roleRepository.findById(20L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRoleId(20L)).thenReturn(List.of(rolePermission));
        when(permissionRepository.findById(30L)).thenReturn(Optional.of(permission));

        assertTrue(service.hasPermission(user, "INVOICE_PROCESS"));
        assertFalse(service.hasPermission(user, "CLIENT_VIEW_ALL"));
    }

    @Test
    void routingSkipsDisabledUsersAndSelectsFirstAuthorizedActiveAccount() {
        User disabled = user(1L, "停用主任");
        disabled.setStatus(0);
        User active = user(2L, "现任主任");
        when(userRepository.findAll()).thenReturn(List.of(disabled, active));
        when(userRoleRepository.findByUserId(2L)).thenReturn(List.of());

        UserPermissionService spy = org.mockito.Mockito.spy(service);
        org.mockito.Mockito.doReturn(true).when(spy)
                .hasPermission(active, "CASE_FILING_FINAL_APPROVE");

        assertEquals(2L, spy.findFirstActiveUserByPermission("CASE_FILING_FINAL_APPROVE").orElseThrow().getId());
    }

    @Test
    void routingPrefersDedicatedCapabilityRoleOverBroadPermissionHolder() {
        User broadPermissionHolder = user(1L, "主任");
        User dedicatedProcessor = user(2L, "专职开票人");
        when(userRepository.findAll()).thenReturn(List.of(broadPermissionHolder, dedicatedProcessor));

        UserPermissionService spy = org.mockito.Mockito.spy(service);
        org.mockito.Mockito.doReturn(true).when(spy)
                .hasPermission(broadPermissionHolder, "INVOICE_PROCESS");
        org.mockito.Mockito.doReturn(true).when(spy)
                .hasPermission(dedicatedProcessor, "INVOICE_PROCESS");
        org.mockito.Mockito.doReturn(false).when(spy)
                .hasRole(broadPermissionHolder, "INVOICE_PROCESSOR");
        org.mockito.Mockito.doReturn(true).when(spy)
                .hasRole(dedicatedProcessor, "INVOICE_PROCESSOR");

        assertEquals(2L, spy.findFirstActiveUserByPermission(
                "INVOICE_PROCESS", "INVOICE_PROCESSOR").orElseThrow().getId());
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username);
        user.setStatus(1);
        user.setDeleted(false);
        return user;
    }
}
