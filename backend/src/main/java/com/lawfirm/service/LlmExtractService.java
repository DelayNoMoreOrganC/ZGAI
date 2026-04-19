package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.OcrExtractRequest;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIFunctionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM智能提取服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmExtractService {

    private final AIConfigService aiConfigService;
    private final AILogService aiLogService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从OCR文本中提取法律要素
     */
    public Map<String, Object> extractLegalElements(OcrExtractRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            // 获取AI配置
            AIConfig config = aiConfigService.getDefaultConfig();
            modelName = config.getModelName();

            // 构建Prompt
            String prompt = buildExtractPrompt(request.getOcrText(), request.getDocumentType());

            // 调用LLM
            String response = callLLM(config, prompt);
            result = response;

            // 解析响应
            Map<String, Object> extracted = parseExtractResponse(response);

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.OCR_RECOGNITION,
                    request.getOcrText(), null, result, null, modelName, status, duration, null);

            return extracted;

        } catch (Exception e) {
            log.error("LLM提取失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.OCR_RECOGNITION,
                    request.getOcrText(), null, null, null, modelName, status, duration, errorMessage);

            throw new RuntimeException("LLM提取失败: " + e.getMessage());
        }
    }

    /**
     * 构建提取Prompt
     */
    private String buildExtractPrompt(String ocrText, String documentType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个法律文书信息提取助手。请从以下法院文书中提取关键信息，以JSON格式返回。\n\n");
        prompt.append("需要提取的字段：\n");
        prompt.append("- caseNumber: 案号\n");
        prompt.append("- courtName: 法院名称\n");
        prompt.append("- hearingDate: 开庭时间(YYYY-MM-DD HH:mm)\n");
        prompt.append("- hearingPlace: 开庭地点/法庭号\n");
        prompt.append("- judgeName: 承办法官姓名\n");
        prompt.append("- clerkName: 书记员姓名\n");
        prompt.append("- plaintiffName: 原告姓名/名称\n");
        prompt.append("- defendantName: 被告姓名/名称\n");
        prompt.append("- caseReason: 案由\n");
        prompt.append("- contactPhone: 联系电话\n");
        prompt.append("- documentType: 文书类型(传票/判决书/裁定书/通知书/其他)\n\n");

        if (documentType != null) {
            prompt.append("文书类型提示：").append(documentType).append("\n\n");
        }

        prompt.append("文书内容：\n");
        prompt.append(ocrText);
        prompt.append("\n\n请严格返回JSON格式，无法识别的字段填null。");

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

    /**
     * 解析提取响应
     */
    private Map<String, Object> parseExtractResponse(String response) {
        try {
            // 尝试直接解析JSON
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            // 如果解析失败，尝试从文本中提取JSON
            try {
                int jsonStart = response.indexOf("{");
                int jsonEnd = response.lastIndexOf("}");
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = response.substring(jsonStart, jsonEnd + 1);
                    return objectMapper.readValue(jsonStr, Map.class);
                }
            } catch (Exception ex) {
                log.error("解析提取响应失败", ex);
            }
            // 返回原始响应
            Map<String, Object> result = new HashMap<>();
            result.put("rawResponse", response);
            return result;
        }
    }
}
