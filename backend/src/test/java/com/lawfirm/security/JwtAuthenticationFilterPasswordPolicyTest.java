package com.lawfirm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.entity.User;
import com.lawfirm.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterPasswordPolicyTest {

    private JwtUtil jwtUtil;
    private UserAuthorityService authorityService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        authorityService = mock(UserAuthorityService.class);
        filter = new JwtAuthenticationFilter(jwtUtil, authorityService, new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksBusinessEndpointsUntilInitialPasswordIsChanged() throws Exception {
        User user = authenticatedUser(true);
        arrangeToken(user);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/cases"), response, chain);

        assertEquals(428, response.getStatus());
        assertTrue(response.getContentAsString().contains("必须先修改密码"));
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allowsPasswordAndIdentityEndpointsWhilePasswordChangeIsRequired() throws Exception {
        User user = authenticatedUser(true);
        arrangeToken(user);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/auth/change-password"), response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allowsBusinessEndpointsAfterPasswordChange() throws Exception {
        User user = authenticatedUser(false);
        arrangeToken(user);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/cases"), response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private void arrangeToken(User user) {
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token")).thenReturn(user.getId());
        when(jwtUtil.getUsernameFromToken("token")).thenReturn(user.getUsername());
        when(authorityService.requireActiveUser(user.getId())).thenReturn(user);
        when(authorityService.loadAuthorities(user)).thenReturn(Collections.emptyList());
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api" + path);
        request.setServletPath(path);
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private User authenticatedUser(boolean mustChangePassword) {
        User user = new User();
        user.setId(8L);
        user.setUsername("测试律师");
        user.setRealName("测试律师");
        user.setPassword("encoded");
        user.setStatus(1);
        user.setDeleted(false);
        user.setMustChangePassword(mustChangePassword);
        return user;
    }
}
