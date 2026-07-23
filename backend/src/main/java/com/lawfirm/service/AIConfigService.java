package com.lawfirm.service;

import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.dto.AIProviderOptionDTO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIProviderType;
import com.lawfirm.repository.AIConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIConfigService {

    private static final Set<String> GENERATION_PROVIDERS = Set.of(
            AIProviderType.LM_STUDIO.name(),
            AIProviderType.DEEPSEEK_API.name(), AIProviderType.GLM_API.name(),
            AIProviderType.KIMI_API.name(), AIProviderType.OPENAI_API.name());

    private final AIConfigRepository aiConfigRepository;
    private final YuandianLegalService yuandianLegalService;
    private final OpenAICompatibleClient openAICompatibleClient;

    /**
     * 创建AI配置
     */
    @Transactional
    public AIConfig createConfig(AIConfigDTO dto) {
        AIConfig config = new AIConfig();
        BeanUtils.copyProperties(dto, config);
        preventLegalDataAsGenerationDefault(config);

        // 如果设置为默认配置，需要取消其他默认配置
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            aiConfigRepository.findByIsDefaultTrueAndDeletedFalse()
                    .ifPresent(c -> c.setIsDefault(false));
        }

        return aiConfigRepository.save(config);
    }

    /**
     * 更新AI配置
     */
    @Transactional
    public AIConfig updateConfig(Long id, AIConfigDTO dto) {
        AIConfig config = aiConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AI配置不存在"));

        BeanUtils.copyProperties(dto, config, "id", "apiKey");
        if (StringUtils.hasText(dto.getApiKey())) {
            config.setApiKey(dto.getApiKey().trim());
        }
        preventLegalDataAsGenerationDefault(config);

        // 如果设置为默认配置，需要取消其他默认配置
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            aiConfigRepository.findByIsDefaultTrueAndDeletedFalse()
                    .filter(c -> !c.getId().equals(id))
                    .ifPresent(c -> c.setIsDefault(false));
        }

        return aiConfigRepository.save(config);
    }

    /**
     * 删除AI配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        AIConfig config = aiConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AI配置不存在"));
        config.setDeleted(true);
        aiConfigRepository.save(config);
    }

    /**
     * 获取AI配置详情
     */
    public AIConfig getConfig(Long id) {
        return aiConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AI配置不存在"));
    }

    /**
     * 获取所有AI配置
     */
    public List<AIConfig> getAllConfigs() {
        return aiConfigRepository.findByDeletedFalse();
    }

    /**
     * 获取默认配置
     */
    public AIConfig getDefaultConfig() {
        return aiConfigRepository.findByIsDefaultTrueAndDeletedFalse()
                .orElseThrow(() -> new RuntimeException("未配置默认AI"));
    }

    public AIConfig getDefaultConfigOrNull() {
        return aiConfigRepository.findByIsDefaultTrueAndDeletedFalse().orElse(null);
    }

    public AIConfig getUsableDefaultConfigOrNull() {
        try {
            return resolveGenerationConfig(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * Empty selection always means the local LM Studio provider. Cloud providers
     * are resolved only when the caller explicitly names one for this request.
     */
    public AIConfig resolveGenerationConfig(String requestedProvider) {
        String provider = StringUtils.hasText(requestedProvider)
                ? requestedProvider.trim().toUpperCase(Locale.ROOT)
                : AIProviderType.LM_STUDIO.name();
        if (!GENERATION_PROVIDERS.contains(provider)) {
            throw new IllegalArgumentException("不支持的生成模型：" + provider);
        }
        AIConfig config = aiConfigRepository
                .findFirstByProviderTypeAndIsEnabledTrueAndDeletedFalseOrderByIsDefaultDescIdAsc(provider)
                .orElseThrow(() -> new IllegalStateException("所选AI服务未启用或未配置：" + provider));
        if (!isUsable(config)) {
            throw new IllegalStateException("所选AI服务缺少连接地址或密钥：" + provider);
        }
        return config;
    }

    public List<AIProviderOptionDTO> getAvailableProviders() {
        return aiConfigRepository.findByDeletedFalse().stream()
                .filter(config -> GENERATION_PROVIDERS.contains(config.getProviderType()))
                .map(config -> new AIProviderOptionDTO(
                        config.getProviderType(),
                        providerDisplayName(config.getProviderType()),
                        config.getModelName(),
                        isUsable(config),
                        isLocal(config.getProviderType()),
                        isLocal(config.getProviderType())
                                ? "请求仅在局域网模型机处理"
                                : "所选请求内容将发送至外部模型服务"))
                .collect(Collectors.toList());
    }

    /**
     * 按提供商类型查找配置
     */
    public List<AIConfig> getConfigsByProvider(String providerType) {
        return aiConfigRepository.findByProviderTypeAndDeletedFalse(providerType);
    }

    /**
     * 测试AI配置连接
     */
    public Map<String, Object> testConnection(Long id) {
        AIConfig config = getConfig(id);
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            String provider = config.getProviderType();
            String apiUrl = config.getApiUrl();
            String apiKey = config.getApiKey();
            String modelName = config.getModelName();

            RestTemplate rt = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            String testContent = "你好，请回复OK表示连接正常。";

            if ("YUANDIAN_LEGAL".equals(provider)) {
                return yuandianLegalService.testConnection();
            } else if ("LM_STUDIO".equals(provider)) {
                return openAICompatibleClient.testConnection(config);
            } else if ("DEEPSEEK_API".equals(provider) || "GLM_API".equals(provider)
                    || "KIMI_API".equals(provider) || "OPENAI_API".equals(provider)) {
                // OpenAI-compatible API
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.setBearerAuth(apiKey);
                }
                Map<String, Object> body = new HashMap<>();
                body.put("model", modelName != null ? modelName : "deepseek-chat");
                body.put("messages", new Object[]{Map.of("role", "user", "content", testContent)});
                body.put("max_tokens", 10);
                org.springframework.http.HttpEntity<Map<String, Object>> req =
                        new org.springframework.http.HttpEntity<>(body, headers);
                org.springframework.http.ResponseEntity<String> resp = rt.postForEntity(apiUrl, req, String.class);
                result.put("status", resp.getStatusCode().is2xxSuccessful() ? "ok" : "error");
                result.put("statusCode", resp.getStatusCodeValue());
            } else {
                // Ollama-compatible API
                String url = (apiUrl != null ? apiUrl : "http://localhost:11434") + "/api/chat";
                Map<String, Object> body = new HashMap<>();
                body.put("model", modelName != null ? modelName : "qwen3:8b");
                body.put("stream", false);
                body.put("options", Map.of("num_predict", 10));
                body.put("messages", new Object[]{Map.of("role", "user", "content", testContent)});
                org.springframework.http.HttpEntity<Map<String, Object>> req =
                        new org.springframework.http.HttpEntity<>(body, headers);
                org.springframework.http.ResponseEntity<String> resp = rt.postForEntity(url, req, String.class);
                result.put("status", resp.getStatusCode().is2xxSuccessful() ? "ok" : "error");
                result.put("statusCode", resp.getStatusCodeValue());
            }

            result.put("duration", System.currentTimeMillis() - startTime);
            result.put("provider", provider);
            result.put("model", modelName);
            result.put("message", "连接测试完成");

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "连接失败: " + e.getMessage());
            result.put("duration", System.currentTimeMillis() - startTime);
        }

        return result;
    }

    private void preventLegalDataAsGenerationDefault(AIConfig config) {
        if (AIProviderType.YUANDIAN_LEGAL.name().equals(config.getProviderType())) {
            config.setIsDefault(false);
        }
    }

    private boolean isUsable(AIConfig config) {
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled()) || !StringUtils.hasText(config.getApiUrl())) {
            return false;
        }
        if (isLocal(config.getProviderType())) {
            return true;
        }
        String key = config.getApiKey();
        return StringUtils.hasText(key) && !key.toLowerCase(Locale.ROOT).contains("your-");
    }

    private boolean isLocal(String provider) {
        return AIProviderType.LM_STUDIO.name().equals(provider)
                || AIProviderType.LOCAL_QWEN.name().equals(provider);
    }

    private String providerDisplayName(String provider) {
        try {
            return AIProviderType.valueOf(provider).getDescription();
        } catch (IllegalArgumentException e) {
            return provider;
        }
    }
}
