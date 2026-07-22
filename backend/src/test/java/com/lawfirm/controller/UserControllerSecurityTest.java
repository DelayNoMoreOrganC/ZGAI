package com.lawfirm.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserControllerSecurityTest {

    @Test
    void accountManagementRequiresUserEditAuthority() throws Exception {
        assertPolicy("createUser", "hasAuthority('USER_EDIT')", com.lawfirm.dto.UserCreateRequest.class);
        assertPolicy("updateUser", "hasAuthority('USER_EDIT')", Long.class, com.lawfirm.dto.UserUpdateRequest.class);
        assertPolicy("deleteUser", "hasAuthority('USER_EDIT')", Long.class);
        assertPolicy("toggleUserStatus", "hasAuthority('USER_EDIT')", Long.class, Integer.class);
        assertPolicy("resetPassword", "hasAuthority('USER_EDIT')", Long.class, java.util.Map.class);
    }

    @Test
    void roleAssignmentRequiresRoleEditAuthority() throws Exception {
        assertPolicy("assignRoles", "hasAuthority('ROLE_EDIT')", Long.class, java.util.Map.class);
    }

    @Test
    void employeeDirectoryAndOwnPasswordRemainAvailableToAuthenticatedUsers() throws Exception {
        assertPolicy("getUserList", "isAuthenticated()",
                int.class, int.class, String.class, Long.class, Integer.class);
        assertPolicy("getUserDetail", "isAuthenticated()", Long.class);
        assertPolicy("changePassword", "isAuthenticated()", java.util.Map.class);
    }

    private void assertPolicy(String methodName, String expected, Class<?>... parameterTypes) throws Exception {
        Method method = UserController.class.getMethod(methodName, parameterTypes);
        PreAuthorize policy = method.getAnnotation(PreAuthorize.class);
        assertNotNull(policy, methodName + " must declare a method security policy");
        assertEquals(expected, policy.value());
    }
}
