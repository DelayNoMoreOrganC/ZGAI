package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * AI文档智能识别服务
 * 实现OCR识别 + LLM要素提取的核心功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIDocumentService {

    private final AIConfigService aiConfigService;
    private final AILogService aiLogService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${ai.ocr.provider:tesseract}")
    private String ocrProvider;

    /**
     * 智能识别法院文书
     * 完整流程：上传→OCR识别→LLM要素提取→返回结果
     */
    public AIDocumentRecognitionResult recognizeLegalDocument(MultipartFile file, Long userId, Long caseId) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取AI配置
            AIConfig aiConfig = aiConfigService.getDefaultConfig();

            // 2. OCR识别
            String ocrText = performOCR(file);
            log.info("OCR识别完成，文本长度: {}", ocrText.length());

            // 3. LLM要素提取
            AIDocumentRecognitionResult result = extractLegalInfo(ocrText, aiConfig);

            // 4. 记录AI使用日志
            // TODO: 实现AI功能后启用日志记录
            /*
            aiLogService.logUsage(
                    userId,
                    caseId,
                    "DOCUMENT_RECOGNITION",
                    ocrText.length(),
                    objectMapper.writeValueAsString(result).length(),
                    aiConfig.getModelName(),
                    "SUCCESS",
                    System.currentTimeMillis() - startTime
            );
            */

            return result;

        } catch (Exception e) {
            log.error("文档识别失败", e);

            // 记录失败日志
            // TODO: 实现AI功能后启用日志记录
            /*
            aiLogService.logUsage(
                    userId,
                    caseId,
                    "DOCUMENT_RECOGNITION",
                    0,
                    0,
                    "unknown",
                    "FAILED",
                    System.currentTimeMillis() - startTime
            );
            */

            throw new AIServiceException("文档识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行OCR识别
     */
    private String performOCR(MultipartFile file) throws Exception {
        if (!ocrEnabled) {
            throw new AIServiceException("OCR功能未启用");
        }

        switch (ocrProvider.toLowerCase()) {
            case "tesseract":
                return performTesseractOCR(file);
            case "baidu":
                return performBaiduOCR(file);
            case "aliyun":
                return performAliyunOCR(file);
            default:
                throw new AIServiceException("不支持的OCR提供商: " + ocrProvider);
        }
    }

    /**
     * Tesseract OCR（本地部署）
     */
    private String performTesseractOCR(MultipartFile file) throws Exception {
        // TODO: 集成本地Tesseract OCR
        // 1. 保存临时文件
        // 2. 调用Tesseract命令行
        // 3. 读取识别结果
        // 4. 删除临时文件

        log.warn("Tesseract OCR尚未实现，返回模拟文本");
        return "模拟OCR文本内容\\n法院名称：北京市朝阳区人民法院\\n案号：（2026）京0105民初1234号\\n原告：张三\\n被告：李四\\n案由：合同纠纷\\n开庭时间：2026年5月20日上午9时30分\\n开庭地点：第三法庭\\n审判长：王法官\\n书记员：赵书记员";
    }

    /**
     * 百度OCR API
     */
    private String performBaiduOCR(MultipartFile file) throws Exception {
        // TODO: 集成百度OCR API
        log.warn("百度OCR尚未实现");
        return "";
    }

    /**
     * 阿里云OCR API
     */
    private String performAliyunOCR(MultipartFile file) throws Exception {
        // TODO: 集成阿里云OCR API
        log.warn("阿里云OCR尚未实现");
        return "";
    }

    /**
     * 使用LLM提取法律文书关键信息
     */
    private AIDocumentRecognitionResult extractLegalInfo(String ocrText, AIConfig aiConfig) throws Exception {
        String prompt = buildExtractionPrompt(ocrText);

        // 调用LLM API
        String llmResponse = callLLM(prompt, aiConfig);

        // 解析LLM返回的JSON
        return parseLLMResponse(llmResponse);
    }

    /**
     * 构建要素提取Prompt
     * TODO: 实现AI文档识别功能需要配置真实LLM API
     */
    private String buildExtractionPrompt(String ocrText) {
        // 暂时返回空字符串，AI功能需要外部服务支持
        return "{\"caseNumber\":null,\"courtName\":null,\"hearingDate\":null,\"hearingPlace\":null,\"judgeName\":null,\"clerkName\":null,\"plaintiffName\":null,\"defendantName\":null,\"caseReason\":null,\"contactPhone\":null,\"documentType\":null}";
        /*
        return String.format("You are a legal document extraction assistant. Extract key information from the following court document and return in JSON format.\n\n" +
                "Fields to extract:\n" +
                "- caseNumber: case number\n" +
                "- courtName: court name\n" +
                "- hearingDate: hearing date (YYYY-MM-DD HH:mm)\n" +
                "- hearingPlace: hearing location/courtroom number\n" +
                "- judgeName: presiding judge name\n" +
                "- clerkName: clerk name\n" +
                "- plaintiffName: plaintiff name\n" +
                "- defendantName: defendant name\n" +
                "- caseReason: case cause/reason\n" +
                "- contactPhone: contact phone\n" +
                "- documentType: document type (summons/judgment/ruling/notice/other)\n\n" +
                "Document content:\n%s\n\n" +
                "Return strict JSON format. Fill null for unrecognized fields. Only return JSON, no other content.",
                ocrText);
        */
    }

    /**
     * 调用LLM API
     */
    private String callLLM(String prompt, AIConfig aiConfig) throws Exception {
        String provider = aiConfig.getProviderType().toLowerCase();

        switch (provider) {
            case "deepseek":
                return callDeepSeekAPI(prompt, aiConfig);
            case "openai":
                return callOpenAIAPI(prompt, aiConfig);
            case "qwen":
                return callQwenAPI(prompt, aiConfig);
            default:
                throw new AIServiceException("不支持的LLM提供商: " + provider);
        }
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekAPI(String prompt, AIConfig config) throws Exception {
        String url = "https://api.deepseek.com/v1/chat/completions";

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModelName());
        requestBody.put("messages", new Object[]{
                new HashMap<String, String>() {{
                    put("role", "user");
                    put("content", prompt);
                }}
        });
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 2000);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            // 解析响应，提取content
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
            Map<String, Object> choices = ((java.util.List<Map<String, Object>>) responseBody.get("choices")).get(0);
            Map<String, Object> message = (Map<String, Object>) choices.get("message");
            return (String) message.get("content");
        } else {
            throw new AIServiceException("DeepSeek API调用失败: " + response.getStatusCode());
        }
    }

    /**
     * 调用OpenAI API
     */
    private String callOpenAIAPI(String prompt, AIConfig config) throws Exception {
        // TODO: 实现OpenAI API调用
        log.warn("OpenAI API尚未实现");
        return "";
    }

    /**
     * 调用通义千问API
     */
    private String callQwenAPI(String prompt, AIConfig config) throws Exception {
        // TODO: 实现通义千问API调用
        log.warn("通义千问API尚未实现");
        return "";
    }

    /**
     * 解析LLM响应
     */
    private AIDocumentRecognitionResult parseLLMResponse(String response) throws Exception {
        try {
            // 尝试直接解析JSON
            return objectMapper.readValue(response, AIDocumentRecognitionResult.class);
        } catch (Exception e) {
            // 如果解析失败，尝试提取JSON部分
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonPart = response.substring(jsonStart, jsonEnd + 1);
                return objectMapper.readValue(jsonPart, AIDocumentRecognitionResult.class);
            }

            throw new AIServiceException("无法解析LLM响应: " + response);
        }
    }
}