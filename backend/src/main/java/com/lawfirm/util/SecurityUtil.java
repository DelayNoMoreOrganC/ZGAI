package com.lawfirm.util;

import com.lawfirm.entity.User;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全工具类 - 获取当前用户信息
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    /**
     * 获取当前用户ID
     *
     * 兼容两种principal类型：
     * 1. Long类型（userId）- JwtAuthenticationFilter设置
     * 2. String类型（username）- 其他情况
     *
     * @return 用户ID
     * @throws RuntimeException 如果用户不存在
     */
    public Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 如果principal已经是Long类型（userId），直接返回
        if (principal instanceof Long) {
            return (Long) principal;
        }

        // 否则尝试通过username查找
        String username = principal.toString();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    public String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 如果principal是Long类型，需要查询数据库获取username
        if (principal instanceof Long) {
            Long userId = (Long) principal;
            return userRepository.findById(userId)
                    .map(User::getUsername)
                    .orElseThrow(() -> new RuntimeException("用户ID不存在: " + userId));
        }

        // 否则直接返回
        return principal.toString();
    }

    /**
     * 获取当前用户实体
     *
     * @return 用户实体
     * @throws RuntimeException 如果用户不存在
     */
    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户ID不存在: " + userId));
    }
}
