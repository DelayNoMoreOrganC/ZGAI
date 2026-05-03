package com.lawfirm.service;

import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.repository.AIConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIConfigService {

    private final AIConfigRepository aiConfigRepository;

    /**
     * 创建AI配置
     */
    @Transactional
    public AIConfig createConfig(AIConfigDTO dto) {
        AIConfig config = new AIConfig();
        BeanUtils.copyProperties(dto, config);

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

        BeanUtils.copyProperties(dto, config, "id");

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
        return aiConfigRepository.findByIsEnabledTrueAndDeletedFalse();
    }

    /**
     * 获取默认配置
     */
    public AIConfig getDefaultConfig() {
        return aiConfigRepository.findByIsDefaultTrueAndDeletedFalse()
                .orElseThrow(() -> new RuntimeException("未配置默认AI"));
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

            if ("DEEPSEEK_API".equals(provider) || "OPENAI_API".equals(provider)) {
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
}
