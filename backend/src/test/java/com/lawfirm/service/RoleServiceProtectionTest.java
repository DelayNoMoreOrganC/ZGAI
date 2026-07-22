package com.lawfirm.service;

import com.lawfirm.dto.RoleCreateRequest;
import com.lawfirm.entity.Role;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.PermissionRepository;
import com.lawfirm.repository.RolePermissionRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleServiceProtectionTest {

    private RoleRepository roleRepository;
    private RoleService service;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        service = new RoleService(
                roleRepository,
                mock(PermissionRepository.class),
                mock(RolePermissionRepository.class),
                mock(UserRoleRepository.class)
        );
    }

    @Test
    void systemRoleCannotBeUpdated() {
        Role role = systemRole("MANAGER");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("MANAGER");
        request.setRoleName("主任");

        assertThrows(InvalidParameterException.class, () -> service.updateRole(1L, request));
    }

    @Test
    void systemRoleCannotBeDeletedOrReassigned() {
        Role role = systemRole("LAWYER");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        assertThrows(InvalidParameterException.class, () -> service.deleteRole(1L));
        assertThrows(InvalidParameterException.class,
                () -> service.assignPermissions(1L, Collections.emptyList()));
    }

    private Role systemRole(String code) {
        Role role = new Role();
        role.setId(1L);
        role.setRoleCode(code);
        role.setRoleName(code);
        role.setDeleted(false);
        return role;
    }
}
