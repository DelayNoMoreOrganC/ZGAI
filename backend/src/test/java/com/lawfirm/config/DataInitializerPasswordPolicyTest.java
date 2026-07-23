package com.lawfirm.config;

import com.lawfirm.entity.SystemConfig;
import com.lawfirm.entity.User;
import com.lawfirm.repository.AIConfigRepository;
import com.lawfirm.repository.CalendarRepository;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.SystemConfigRepository;
import com.lawfirm.repository.TodoRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerPasswordPolicyTest {

    @Test
    void oneTimeMigrationMarksEmployeesButKeepsDeveloperAccountsAvailable() {
        UserRepository users = mock(UserRepository.class);
        AIConfigRepository aiConfigs = mock(AIConfigRepository.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        SystemConfigRepository systemConfigs = mock(SystemConfigRepository.class);
        User admin = user("admin");
        User legacyAdmin = user("amin");
        User employee = user("测试律师");

        when(users.count()).thenReturn(3L);
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(users.findAll()).thenReturn(Arrays.asList(admin, legacyAdmin, employee));
        when(aiConfigs.count()).thenReturn(1L);
        when(aiConfigs.findByProviderTypeAndDeletedFalse(anyString())).thenReturn(Collections.emptyList());
        when(aiConfigs.findByIsDefaultTrueAndDeletedFalse()).thenReturn(Optional.empty());
        when(systemConfigs.existsByConfigKey("security.password-change-policy.v1")).thenReturn(false);

        DataInitializer initializer = new DataInitializer(
                users,
                mock(RoleRepository.class),
                mock(UserRoleRepository.class),
                mock(PasswordEncoder.class),
                mock(TodoRepository.class),
                mock(CalendarRepository.class),
                aiConfigs,
                departments,
                systemConfigs);

        initializer.run();

        assertFalse(admin.getMustChangePassword());
        assertFalse(legacyAdmin.getMustChangePassword());
        assertTrue(employee.getMustChangePassword());
        ArgumentCaptor<SystemConfig> marker = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigs).save(marker.capture());
        assertTrue(marker.getValue().getConfigKey().contains("password-change-policy"));
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setRealName(username);
        user.setPassword("encoded");
        user.setPosition("律师");
        user.setStatus(1);
        user.setDeleted(false);
        user.setMustChangePassword(false);
        return user;
    }
}
