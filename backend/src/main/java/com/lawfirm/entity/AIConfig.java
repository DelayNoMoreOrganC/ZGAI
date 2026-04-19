package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_config")
public class AIConfig extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "配置名称不能为空")
    @Column(name = "config_name", nullable = false)
    private String configName;

    @NotNull(message = "AI提供商不能为空")
    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "api_url", length = 500)
    private String apiUrl;

    @Column(name = "model_name", length = 100)
    private String modelName;

    private String systemPrompt;

    @Column(name = "temperature")
    private Double temperature = 0.7;

    @Column(name = "max_tokens")
    private Integer maxTokens = 2000;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 30;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(length = 20)
    private String category;

    private String description;
}
