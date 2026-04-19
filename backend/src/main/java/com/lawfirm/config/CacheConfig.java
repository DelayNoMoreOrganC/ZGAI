package com.lawfirm.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置
 * 性能优化：为频繁访问的数据添加缓存支持
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存管理器
     * 使用ConcurrentMap实现内存缓存
     */
    @Bean
    public CacheManager cacheManager() {
        // 定义多个缓存区域
        return new ConcurrentMapCacheManager(
            "users",           // 用户信息缓存
            "roles",           // 角色信息缓存
            "systemConfig",    // 系统配置缓存
            "aiConfig",        // AI配置缓存
            "clients",         // 客户信息缓存
            "cases",           // 案件信息缓存
            "statistics"       // 统计数据缓存（短期缓存）
        );
    }
}