package com.lawfirm.repository;

import com.lawfirm.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置Repository
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * 根据配置键查找配置
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 根据分类查找配置列表
     */
    List<SystemConfig> findByCategory(String category);

    /**
     * 检查配置键是否存在
     */
    boolean existsByConfigKey(String configKey);
}
