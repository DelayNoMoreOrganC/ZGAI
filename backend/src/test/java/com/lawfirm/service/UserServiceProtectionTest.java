package com.lawfirm.service;

import com.lawfirm.entity.User;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceProtectionTest {

    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        service = new UserService(
                userRepository,
                mock(DepartmentRepository.class),
                mock(RoleRepository.class),
                userRoleRepository,
                mock(PasswordEncoder.class)
        );
    }

    @Test
    void developmentAdminCannotBeDeleted() {
        User admin = developmentAdmin("admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(InvalidParameterException.class, () -> service.deleteUser(1L));
    }

    @Test
    void developmentAdminCannotBeDisabled() {
        User admin = developmentAdmin("amin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(InvalidParameterException.class, () -> service.toggleUserStatus(1L, 0));
    }

    @Test
    void developmentAdminRolesCannotBeReassigned() {
        User admin = developmentAdmin("admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(InvalidParameterException.class,
                () -> service.assignRoles(1L, Collections.emptyList()));
        verifyNoInteractions(userRoleRepository);
    }

    private User developmentAdmin(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setRealName("开发管理员");
        user.setPassword("encoded");
        user.setStatus(1);
        return user;
    }
}
