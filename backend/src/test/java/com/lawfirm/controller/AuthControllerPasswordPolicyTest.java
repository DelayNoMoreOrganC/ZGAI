package com.lawfirm.controller;

import com.lawfirm.entity.User;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.security.UserAuthorityService;
import com.lawfirm.util.JwtUtil;
import com.lawfirm.util.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerPasswordPolicyTest {

    private UserRepository userRepository;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;
    private UserAuthorityService authorityService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtUtil = mock(JwtUtil.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authorityService = mock(UserAuthorityService.class);
        controller = new AuthController(
                mock(AuthenticationManager.class),
                userRepository,
                jwtUtil,
                passwordEncoder,
                authorityService);
    }

    @Test
    void currentUserResponseExposesPasswordChangeRequirement() {
        User user = user(true);
        when(jwtUtil.getUserIdFromToken("token")).thenReturn(12L);
        when(authorityService.requireActiveUser(12L)).thenReturn(user);
        when(authorityService.loadAuthorities(user)).thenReturn(Collections.emptyList());

        Result<Map<String, Object>> result = controller.getCurrentUser("Bearer token");

        assertEquals(Boolean.TRUE, result.getData().get("mustChangePassword"));
    }

    @Test
    void changingPasswordClearsRequirement() {
        User user = user(true);
        when(jwtUtil.getUserIdFromToken("token")).thenReturn(12L);
        when(userRepository.findById(12L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-pass-123", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-pass-123")).thenReturn("new-hash");
        AuthController.ChangePasswordRequest request = new AuthController.ChangePasswordRequest();
        request.setOldPassword("old-pass");
        request.setNewPassword("new-pass-123");

        controller.changePassword("Bearer token", request);

        assertFalse(user.getMustChangePassword());
        verify(userRepository).save(user);
    }

    private User user(boolean mustChangePassword) {
        User user = new User();
        user.setId(12L);
        user.setUsername("测试律师");
        user.setRealName("测试律师");
        user.setPassword("old-hash");
        user.setStatus(1);
        user.setDeleted(false);
        user.setMustChangePassword(mustChangePassword);
        return user;
    }
}
