package com.lawfirm.controller;

import com.lawfirm.entity.User;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.security.UserAuthorityService;
import com.lawfirm.util.JwtUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerAuthorityMappingTest {

    @Test
    void rolePrefixedPermissionsRemainPermissions() {
        UserAuthorityService authorityService = mock(UserAuthorityService.class);
        AuthController controller = new AuthController(
                mock(AuthenticationManager.class),
                mock(UserRepository.class),
                mock(JwtUtil.class),
                mock(PasswordEncoder.class),
                authorityService);
        User user = new User();
        user.setId(1L);
        when(authorityService.loadRoleCodes(user)).thenReturn(List.of("ADMIN"));
        Map<String, Object> data = new HashMap<>();

        controller.addAuthorityData(data, user,
                List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_VIEW", "ROLE_EDIT", "CASE_VIEW"));

        assertEquals(List.of("ADMIN"), data.get("roles"));
        assertEquals(List.of("ROLE_VIEW", "ROLE_EDIT", "CASE_VIEW"), data.get("permissions"));
    }
}
