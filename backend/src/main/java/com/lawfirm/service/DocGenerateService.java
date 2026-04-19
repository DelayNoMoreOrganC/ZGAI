package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.DocGenerateRequest;
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
 * AI文书生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocGenerateService {

    private final AIConfigService aiConfigService;
    private final AILogService aiLogService;
    private final CaseRepository caseRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成法律文书
     */
    public String generateDocument(DocGenerateRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            // 获取AI配置
            AIConfig config = aiConfigService.getDefaultConfig();
            modelName = config.getModelName();

            // 获取案件信息
            Case caseEntity = caseRepository.findById(request.getCaseId())
                    .orElseThrow(() -> new RuntimeException("案件不存在"));

            // 构建Prompt
            String prompt = buildDocumentPrompt(caseEntity, request.getDocumentType(),
                    request.getCustomPrompt(), request.getAdditionalContext());

            // 调用LLM
            String response = callLLM(config, prompt);
            result = response;

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.DOCUMENT_GENERATION,
                    prompt, null, result, null, modelName, status, duration, null);

            return result;

        } catch (Exception e) {
            log.error("文书生成失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.DOCUMENT_GENERATION,
                    null, null, null, null, modelName, status, duration, errorMessage);

            throw new RuntimeException("文书生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建文书生成Prompt
     */
    private String buildDocumentPrompt(Case caseEntity, String documentType,
                                       String customPrompt, String additionalContext) {
        StringBuilder prompt = new StringBuilder();

        switch (documentType) {
            case "COMPLAINT":
                prompt.append("请根据以下案件信息，起草一份起诉状。\n\n");
                prompt.append("案件名称：").append(caseEntity.getCaseName()).append("\n");
                prompt.append("案由：").append(caseEntity.getCaseReason()).append("\n");
                prompt.append("案件类型：").append(caseEntity.getCaseType()).append("\n");
                prompt.append("诉讼请求：\n");
                if (customPrompt != null) {
                    prompt.append(customPrompt).append("\n");
                }
                prompt.append("\n请按照标准起诉状格式起草，包括：原告信息、被告信息、诉讼请求、事实与理由、证据和证据来源等。");
                break;

            case "DEFENSE_STATEMENT":
                prompt.append("请根据以下案件信息，起草一份答辩状。\n\n");
                prompt.append("案件名称：").append(caseEntity.getCaseName()).append("\n");
                prompt.append("案由：").append(caseEntity.getCaseReason()).append("\n");
                prompt.append("案件简述：").append(caseEntity.getSummary()).append("\n");
                if (customPrompt != null) {
                    prompt.append("答辩意见：").append(customPrompt).append("\n");
                }
                prompt.append("\n请按照标准答辩状格式起草，针对原告的诉讼请求逐一进行答辩。");
                break;

            case "BRIEF":
                prompt.append("请根据以下案件信息，起草一份代理词。\n\n");
                prompt.append("案件名称：").append(caseEntity.getCaseName()).append("\n");
                prompt.append("案由：").append(caseEntity.getCaseReason()).append("\n");
                prompt.append("案件简述：").append(caseEntity.getSummary()).append("\n");
                if (customPrompt != null) {
                    prompt.append("代理意见：").append(customPrompt).append("\n");
                }
                prompt.append("\n请按照标准代理词格式起草，包括：案件基本情况、争议焦点、法律分析、代理意见等。");
                break;

            case "LEGAL_OPINION":
                prompt.append("请根据以下案件信息，起草一份法律意见书。\n\n");
                prompt.append("案件名称：").append(caseEntity.getCaseName()).append("\n");
                prompt.append("案由：").append(caseEntity.getCaseReason()).append("\n");
                prompt.append("案件简述：").append(caseEntity.getSummary()).append("\n");
                if (customPrompt != null) {
                    prompt.append("咨询问题：").append(customPrompt).append("\n");
                }
                prompt.append("\n请按照标准法律意见书格式起草，包括：案件背景、法律分析、风险评估、建议等。");
                break;

            default:
                prompt.append("请根据以下案件信息，起草一份").append(documentType).append("。\n\n");
                prompt.append("案件名称：").append(caseEntity.getCaseName()).append("\n");
                prompt.append("案由：").append(caseEntity.getCaseReason()).append("\n");
                break;
        }

        if (additionalContext != null) {
            prompt.append("\n\n补充信息：\n").append(additionalContext);
        }

        return prompt.toString();
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
                return root.path("choices").get(0).path("message").path("content").asText();
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
                return root.path("choices").get(0).path("message").path("content").asText();
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
