package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI配置DTO
 */
@Data
public class AIConfigDTO {

    @NotBlank(message = "配置名称不能为空")
    private String configName;

    @NotNull(message = "AI提供商不能为空")
    private String providerType;

    private String apiKey;

    private String apiUrl;

    private String modelName;

    private String systemPrompt;

    private Double temperature = 0.7;

    private Integer maxTokens = 2000;

    private Integer timeoutSeconds = 30;

    private Boolean isDefault = false;

    private Boolean isEnabled = true;

    private String category;

    private String description;
}
