package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AiChatRequest;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.entity.Case;
import com.lawfirm.enums.AIFunctionType;
import com.lawfirm.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * AI问答服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AIConfigService aiConfigService;
    private final AILogService aiLogService;
    private final CaseRepository caseRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通用法律问答
     */
    public String generalChat(String message, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            // 获取AI配置
            AIConfig config = aiConfigService.getDefaultConfig();
            if (config == null) {
                throw new RuntimeException("AI配置未设置，请先在系统设置中配置AI服务");
            }
            modelName = config.getModelName();

            // 构建Prompt
            String prompt = buildGeneralChatPrompt(message);

            // 调用LLM
            String response = callLLM(config, prompt);
            result = response;

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, null, AIFunctionType.LEGAL_QA,
                    message, null, result, null, modelName, status, duration, null);

            return result;

        } catch (Exception e) {
            log.error("AI问答失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, null, AIFunctionType.LEGAL_QA,
                    message, null, null, null, modelName, status, duration, errorMessage);

            throw new RuntimeException("AI问答失败: " + e.getMessage());
        }
    }

    /**
     * 案件上下文问答
     */
    public String caseChat(AiChatRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            // 获取AI配置
            AIConfig config = aiConfigService.getDefaultConfig();
            if (config == null) {
                throw new RuntimeException("AI配置未设置，请先在系统设置中配置AI服务");
            }
            modelName = config.getModelName();

            // 获取案件信息
            Case caseEntity = null;
            String caseContext = "";
            if (request.getCaseId() != null) {
                caseEntity = caseRepository.findById(request.getCaseId())
                        .orElseThrow(() -> new RuntimeException("案件不存在: " + request.getCaseId()));
                caseContext = buildCaseContext(caseEntity);
            }

            // 构建Prompt
            String prompt = buildCaseChatPrompt(request.getMessage(), caseContext);

            // 调用LLM
            String response = callLLM(config, prompt);
            result = response;

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.CASE_ANALYSIS,
                    request.getMessage(), null, result, null, modelName, status, duration, null);

            return result;

        } catch (Exception e) {
            log.error("案件AI问答失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.CASE_ANALYSIS,
                    request.getMessage(), null, null, null, modelName, status, duration, errorMessage);

            throw new RuntimeException("案件AI问答失败: " + e.getMessage());
        }
    }

    /**
     * 构建通用问答Prompt
     */
    private String buildGeneralChatPrompt(String message) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的法律助手，请根据用户的问题提供准确的法律建议。\n\n");
        prompt.append("用户问题：").append(message).append("\n\n");
        prompt.append("请提供专业、准确的法律建议。如果问题涉及具体案件，建议用户提供更多详细信息。");
        return prompt.toString();
    }

    /**
     * 构建案件问答Prompt
     */
    private String buildCaseChatPrompt(String message, String caseContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的法律助手，请根据案件信息回答用户的问题。\n\n");

        if (caseContext != null && !caseContext.isEmpty()) {
            prompt.append("案件信息：\n").append(caseContext).append("\n\n");
        }

        prompt.append("用户问题：").append(message).append("\n\n");
        prompt.append("请基于案件信息提供专业、准确的法律建议。");

        return prompt.toString();
    }

    /**
     * 构建案件上下文
     */
    private String buildCaseContext(Case caseEntity) {
        StringBuilder context = new StringBuilder();
        context.append("案件名称：").append(caseEntity.getCaseName()).append("\n");
        context.append("案号：").append(caseEntity.getCaseNumber()).append("\n");
        context.append("案件类型：").append(caseEntity.getCaseType()).append("\n");
        context.append("案由：").append(caseEntity.getCaseReason()).append("\n");
        context.append("管辖法院：").append(caseEntity.getCourt()).append("\n");
        if (caseEntity.getSummary() != null) {
            context.append("案件简述：").append(caseEntity.getSummary()).append("\n");
        }
        context.append("案件状态：").append(caseEntity.getStatus()).append("\n");
        return context.toString();
    }

    /**
     * 调用LLM API
     */
    private String callLLM(AIConfig config, String prompt) {
        try {
            String apiUrl = config.getApiUrl();
            String apiKey = config.getApiKey();

            if ("DEEPSEEK_API".equals(config.getProviderType())) {
                return callDeepSeek(apiUrl, apiKey, prompt, config);
            } else if ("OPENAI_API".equals(config.getProviderType())) {
                return callOpenAI(apiUrl, apiKey, prompt, config);
            } else if ("LOCAL_QWEN".equals(config.getProviderType())) {
                return callLocalQwen(apiUrl, prompt, config);
            } else {
                throw new RuntimeException("不支持的AI提供商: " + config.getProviderType());
            }
        } catch (Exception e) {
            log.error("调用LLM API失败", e);
            throw new RuntimeException("调用LLM API失败: " + e.getMessage());
        }
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeek(String apiUrl, String apiKey, String prompt, AIConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModelName() != null ? config.getModelName() : "deepseek-chat");
            requestBody.put("messages", new Object[]{
                    Map.of("role", "user", "content", prompt)
            });
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl != null ? apiUrl : "https://api.deepseek.com/v1/chat/completions",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    return choices.get(0).path("message").path("content").asText();
                }
                return "";
            } else {
                throw new RuntimeException("DeepSeek API返回错误: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            throw new RuntimeException("调用DeepSeek API失败: " + e.getMessage());
        }
    }

    /**
     * 调用OpenAI API
     */
    private String callOpenAI(String apiUrl, String apiKey, String prompt, AIConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModelName() != null ? config.getModelName() : "gpt-3.5-turbo");
            requestBody.put("messages", new Object[]{
                    Map.of("role", "user", "content", prompt)
            });
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl != null ? apiUrl : "https://api.openai.com/v1/chat/completions",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    return choices.get(0).path("message").path("content").asText();
                }
                return "";
            } else {
                throw new RuntimeException("OpenAI API返回错误: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("调用OpenAI API失败", e);
            throw new RuntimeException("调用OpenAI API失败: " + e.getMessage());
        }
    }

    /**
     * 调用本地Qwen
     */
    private String callLocalQwen(String apiUrl, String prompt, AIConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", prompt);
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl != null ? apiUrl : "http://localhost:8000/generate",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("response").asText();
            } else {
                throw new RuntimeException("本地Qwen服务返回错误: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("调用本地Qwen失败", e);
            throw new RuntimeException("调用本地Qwen失败: " + e.getMessage());
        }
    }
}
