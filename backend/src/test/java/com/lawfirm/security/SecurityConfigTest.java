package com.lawfirm.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void corsAllowsPatchForPartialResourceUpdates() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class), mock(JwtAuthenticationEntryPoint.class));

        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/archive-jobs/1/documents"));

        assertNotNull(configuration);
        assertTrue(configuration.getAllowedMethods().contains("PATCH"));
    }
}
