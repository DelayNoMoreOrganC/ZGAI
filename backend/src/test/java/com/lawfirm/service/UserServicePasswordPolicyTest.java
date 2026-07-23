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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServicePasswordPolicyTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserService(
                userRepository,
                mock(DepartmentRepository.class),
                mock(RoleRepository.class),
                mock(UserRoleRepository.class),
                passwordEncoder);
    }

    @Test
    void administratorResetRequiresTheEmployeeToChangePasswordAgain() {
        User user = user("普通律师", false);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("reset123")).thenReturn("reset-hash");

        service.resetPassword(9L, "reset123");

        assertTrue(user.getMustChangePassword());
        verify(userRepository).save(user);
    }

    @Test
    void successfulSelfServiceChangeClearsTheRequirement() {
        User user = user("普通律师", true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-pass-123", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-pass-123")).thenReturn("new-hash");

        service.changePassword(9L, "old-pass", "new-pass-123");

        assertFalse(user.getMustChangePassword());
        verify(userRepository).save(user);
    }

    @Test
    void changingToTheCurrentPasswordIsRejected() {
        User user = user("普通律师", true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("same-password", "old-hash")).thenReturn(true);

        assertThrows(InvalidParameterException.class,
                () -> service.changePassword(9L, "same-password", "same-password"));
    }

    @Test
    void protectedDeveloperAccountIsNotLockedByAnAdministrativeReset() {
        User admin = user("admin", false);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("reset123")).thenReturn("reset-hash");

        service.resetPassword(9L, "reset123");

        assertFalse(admin.getMustChangePassword());
    }

    private User user(String username, boolean mustChangePassword) {
        User user = new User();
        user.setId(9L);
        user.setUsername(username);
        user.setRealName(username);
        user.setPassword("old-hash");
        user.setStatus(1);
        user.setDeleted(false);
        user.setMustChangePassword(mustChangePassword);
        return user;
    }
}
