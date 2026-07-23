package com.lawfirm.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EmployeePermissionMatrixTest {

    private final EmployeePermissionSyncService service = new EmployeePermissionSyncService(
            mock(com.lawfirm.repository.DepartmentRepository.class),
            mock(com.lawfirm.repository.UserRepository.class),
            mock(com.lawfirm.repository.RoleRepository.class),
            mock(com.lawfirm.repository.PermissionRepository.class),
            mock(com.lawfirm.repository.UserRoleRepository.class),
            mock(com.lawfirm.repository.RolePermissionRepository.class),
            mock(org.springframework.security.crypto.password.PasswordEncoder.class));

    @Test
    void mapsLegalManagementPositionsToLegalRoles() {
        assertEquals(List.of("MANAGER", "LAWYER"), service.roleCodesForPosition("主任"));
        assertEquals(List.of("DEPT_HEAD", "LAWYER"), service.roleCodesForPosition("主管"));
        assertEquals(List.of("DEPT_HEAD", "LAWYER"), service.roleCodesForPosition("部门主管"));
    }

    @Test
    void mapsAdministrativeAndFinanceAliasesWithoutFallingBackToAssistant() {
        assertEquals(List.of("ADMINISTRATIVE"), service.roleCodesForPosition("行政管理"));
        assertEquals(List.of("ADMINISTRATIVE"), service.roleCodesForPosition("行政"));
        assertEquals(List.of("FINANCE"), service.roleCodesForPosition("财务"));
        assertEquals(List.of("FINANCE"), service.roleCodesForPosition("出纳"));
    }

    @Test
    void keepsOperationalPermissionBoundariesDistinct() {
        List<String> employee = service.employeePermissions();
        List<String> administrative = service.administrativePermissions();
        List<String> director = service.allPermissions();

        assertTrue(employee.contains("CASE_EDIT"));
        assertFalse(administrative.contains("CASE_EDIT"));
        assertTrue(administrative.contains("APPROVAL_EDIT"));
        assertTrue(administrative.contains("SEAL_APPROVE"));
        assertFalse(employee.contains("SEAL_APPROVE"));
        assertTrue(administrative.contains("CASE_FILING_REVIEW"));
        assertTrue(administrative.contains("CASE_ARCHIVE_REVIEW"));
        assertFalse(employee.contains("CASE_ARCHIVE_REVIEW"));
        assertTrue(administrative.contains("CLIENT_VIEW_ALL"));
        assertFalse(employee.contains("SYSTEM_CONFIG"));
        assertFalse(administrative.contains("SYSTEM_CONFIG"));
        assertTrue(director.contains("SYSTEM_CONFIG"));
        assertTrue(director.contains("AI_CONFIG"));
        assertTrue(director.contains("CASE_FILING_FINAL_APPROVE"));
    }
}
