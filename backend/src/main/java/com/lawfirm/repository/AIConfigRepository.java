package com.lawfirm.repository;

import com.lawfirm.entity.AIConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI配置Repository
 */
@Repository
public interface AIConfigRepository extends JpaRepository<AIConfig, Long> {

    /**
     * 查找默认配置
     */
    Optional<AIConfig> findByIsDefaultTrueAndDeletedFalse();

    /**
     * 按提供商类型查找配置
     */
    List<AIConfig> findByProviderTypeAndDeletedFalse(String providerType);

    /**
     * 查找所有启用的配置
     */
    List<AIConfig> findByIsEnabledTrueAndDeletedFalse();
}
