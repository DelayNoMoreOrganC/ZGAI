package com.lawfirm.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 生成JWT Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return Token字符串
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return generateToken(claims);
    }

    /**
     * 生成JWT Token
     *
     * @param claims 自定义声明
     * @return Token字符串
     */
    public String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignKey())
                .compact();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token Token字符串
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            throw new RuntimeException("无效的认证令牌");
        }
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            throw new RuntimeException("令牌中缺少用户ID");
        }
        return userId;
    }

    /**
     * 从Token中获取用户名
     *
     * @param token Token字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    /**
     * 验证Token是否有效
     *
     * @param token Token字符串
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException e) {
            log.error("JWT签名验证失败: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT格式错误: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT参数错误: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 获取Token过期时间
     *
     * @param token Token字符串
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 判断Token是否即将过期（剩余时间小于30分钟）
     *
     * @param token Token字符串
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return false;
        }
        long diff = expiration.getTime() - System.currentTimeMillis();
        return diff < (30 * 60 * 1000); // 30分钟
    }

    /**
     * 从Token中获取Claims
     *
     * @param token Token字符串
     * @return Claims对象
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("解析JWT失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取签名密钥
     *
     * @return 密钥
     */
    private SecretKey getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
