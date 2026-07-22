package com.lawfirm.dto;

import com.lawfirm.entity.AIConfig;
import lombok.Data;

/**
 * AI configuration view without provider secrets.
 */
@Data
public class AIConfigVO {
    private Long id;
    private String configName;
    private String providerType;
    private Boolean apiKeyConfigured;
    private String apiUrl;
    private String modelName;
    private String systemPrompt;
    private Double temperature;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Boolean isDefault;
    private Boolean isEnabled;
    private String category;
    private String description;

    public static AIConfigVO from(AIConfig config) {
        AIConfigVO view = new AIConfigVO();
        view.setId(config.getId());
        view.setConfigName(config.getConfigName());
        view.setProviderType(config.getProviderType());
        view.setApiKeyConfigured(config.getApiKey() != null && !config.getApiKey().trim().isEmpty());
        view.setApiUrl(config.getApiUrl());
        view.setModelName(config.getModelName());
        view.setSystemPrompt(config.getSystemPrompt());
        view.setTemperature(config.getTemperature());
        view.setMaxTokens(config.getMaxTokens());
        view.setTimeoutSeconds(config.getTimeoutSeconds());
        view.setIsDefault(config.getIsDefault());
        view.setIsEnabled(config.getIsEnabled());
        view.setCategory(config.getCategory());
        view.setDescription(config.getDescription());
        return view;
    }
}
