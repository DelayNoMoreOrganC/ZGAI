package com.lawfirm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.entity.User;
import com.lawfirm.util.JwtUtil;
import com.lawfirm.util.Result;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserAuthorityService userAuthorityService;
    private final ObjectMapper objectMapper;

    private static final String HEADER_NAME = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 设置请求和响应的字符编码为UTF-8
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                Long userId = jwtUtil.getUserIdFromToken(jwt);
                String username = jwtUtil.getUsernameFromToken(jwt);

                if (userId != null && username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userAuthorityService.requireActiveUser(userId);
                    if (!username.equals(user.getUsername())) {
                        throw new IllegalArgumentException("令牌用户信息不一致");
                    }
                    List<SimpleGrantedAuthority> authorities = userAuthorityService.loadAuthorities(user);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("已设置用户认证信息: userId={}, username={}, authorities={}", userId, username, authorities.size());

                    if (Boolean.TRUE.equals(user.getMustChangePassword()) && !isPasswordPolicyEndpoint(request)) {
                        response.setStatus(428);
                        objectMapper.writeValue(response.getWriter(),
                                Result.error(428, "首次登录或密码重置后必须先修改密码"));
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.error("无法设置用户认证: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPasswordPolicyEndpoint(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return "/auth/login".equals(path)
                || "/auth/logout".equals(path)
                || "/auth/current-user".equals(path)
                || "/auth/change-password".equals(path)
                || "/users/change-password".equals(path)
                || "/health".equals(path)
                || path.startsWith("/error");
    }

    /**
     * 从请求中提取JWT Token
     *
     * @param request HTTP请求
     * @return JWT Token字符串（不含前缀）
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_NAME);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
