package com.lawfirm.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 字符编码过滤器 - 确保所有请求使用UTF-8编码
 * 优先级设置为最高，确保在所有其他Filter之前执行
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class Utf8EncodingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 强制设置请求和响应的字符编码为UTF-8
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        // 确保响应头明确声明UTF-8编码
        response.setContentType("application/json;charset=UTF-8");

        // 继续执行Filter链
        filterChain.doFilter(request, response);
    }
}
