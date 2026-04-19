package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG Knowledge Service (MVP - Keyword Search)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGKnowledgeService {

    private final AIConfigService aiConfigService;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * RAG search and answer
     */
    public Map<String, Object> ragSearch(String question, Long userId) {
        log.info("RAG search question: {}", question);

        try {
            // Step 1: Keyword search relevant documents
            List<KnowledgeArticle> relevantDocs = searchRelevantDocuments(question);

            if (relevantDocs.isEmpty()) {
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("answer", "No relevant documents found. Please try other keywords.");
                emptyResult.put("sources", List.of());
                emptyResult.put("hasAnswer", false);
                return emptyResult;
            }

            // Step 2: Build context
            String context = buildContext(relevantDocs);

            // Step 3: Generate answer via LLM
            String answer = generateAnswer(question, context);

            // Step 4: Extract source info
            List<Map<String, Object>> sources = relevantDocs.stream()
                .limit(3)
                .map(doc -> {
                    Map<String, Object> sourceInfo = new HashMap<>();
                    sourceInfo.put("id", doc.getId());
                    sourceInfo.put("title", doc.getTitle());
                    sourceInfo.put("category", doc.getCategory());

                    String summary = doc.getSummary();
                    if (summary == null && doc.getContent() != null) {
                        String content = doc.getContent();
                        summary = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                    }
                    sourceInfo.put("summary", summary != null ? summary : "");

                    return sourceInfo;
                })
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("sources", sources);
            result.put("hasAnswer", true);
            result.put("documentCount", relevantDocs.size());
            return result;

        } catch (Exception e) {
            log.error("RAG search failed", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("answer", "System temporarily unavailable. Please try again later.");
            errorResult.put("error", e.getMessage());
            errorResult.put("hasAnswer", false);
            return errorResult;
        }
    }

    /**
     * Search relevant documents by keywords
     */
    private List<KnowledgeArticle> searchRelevantDocuments(String question) {
        String[] keywords = question.split("[\\s\\p{Punct}]+");
        List<KnowledgeArticle> results = new ArrayList<>();

        for (String keyword : keywords) {
            if (keyword.length() < 2) continue;

            try {
                Page<KnowledgeArticle> page = knowledgeArticleRepository
                    .searchArticles(keyword, null);
                results.addAll(page.getContent());
            } catch (Exception e) {
                log.warn("Search failed for keyword: {}", keyword, e);
            }
        }

        return results.stream()
            .distinct()
            .sorted((a, b) -> {
                return b.getUpdatedAt() != null && a.getUpdatedAt() != null ?
                    b.getUpdatedAt().compareTo(a.getUpdatedAt()) : 0;
            })
            .limit(5)
            .collect(Collectors.toList());
    }

    /**
     * Build context from documents
     */
    private String buildContext(List<KnowledgeArticle> docs) {
        StringBuilder context = new StringBuilder();
        context.append("Knowledge Base Documents:\n\n");

        for (int i = 0; i < docs.size(); i++) {
            KnowledgeArticle doc = docs.get(i);
            context.append(String.format("[Doc%d] %s\n", i + 1, doc.getTitle()));
            context.append(String.format("Category: %s\n", doc.getCategory()));

            if (doc.getSummary() != null) {
                context.append(String.format("Summary: %s\n", doc.getSummary()));
            } else if (doc.getContent() != null) {
                String content = doc.getContent();
                context.append(String.format("Content: %s\n",
                    content.length() > 500 ? content.substring(0, 500) + "..." : content));
            }
            context.append("\n");
        }

        return context.toString();
    }

    /**
     * Generate answer via LLM
     */
    private String generateAnswer(String question, String context) {
        try {
            AIConfig config = aiConfigService.getDefaultConfig();
            if (config == null) {
                return "AI service not configured.";
            }

            String prompt = buildPrompt(question, context);
            String response = callLLMAPI(config, prompt);
            return extractAnswer(response);

        } catch (Exception e) {
            log.error("LLM call failed", e);
            return "Answer generation failed. Please try again later.";
        }
    }

    /**
     * Build RAG prompt
     */
    private String buildPrompt(String question, String context) {
        return String.format(
            "You are a professional legal assistant. Please answer the user's question based on the following knowledge base documents.\n\n" +
            "%s\n\n" +
            "User Question: %s\n\n" +
            "Requirements:\n" +
            "1. Answer based ONLY on the above documents, do not fabricate information\n" +
            "2. If no answer in documents, clearly inform the user\n" +
            "3. Answer should be accurate, professional and easy to understand\n" +
            "4. Cite specific content from documents when necessary\n" +
            "5. Use clear format with paragraphs and lists\n\n" +
            "Please answer:",
            context, question
        );
    }

    /**
     * Call LLM API
     */
    private String callLLMAPI(AIConfig config, String prompt) {
        String apiUrl = config.getApiUrl();
        String apiKey = config.getApiKey();
        String providerType = config.getProviderType();

        if ("DEEPSEEK_API".equals(providerType)) {
            return callDeepSeek(apiUrl, apiKey, prompt, config);
        } else if ("OPENAI_API".equals(providerType)) {
            return callOpenAI(apiUrl, apiKey, prompt, config);
        } else {
            throw new RuntimeException("Unsupported AI provider: " + providerType);
        }
    }

    /**
     * Call DeepSeek API
     */
    private String callDeepSeek(String apiUrl, String apiKey, String prompt, AIConfig config) {
        try {
            String url = apiUrl + "/chat/completions";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModelName() != null ? config.getModelName() : "deepseek-chat");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7);
            requestBody.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 2000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("DeepSeek API error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("DeepSeek API call failed", e);
            throw new RuntimeException("DeepSeek API call failed: " + e.getMessage());
        }
    }

    /**
     * Call OpenAI API
     */
    private String callOpenAI(String apiUrl, String apiKey, String prompt, AIConfig config) {
        try {
            String url = apiUrl + "/chat/completions";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModelName() != null ? config.getModelName() : "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7);
            requestBody.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 2000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("OpenAI API error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage());
        }
    }

    /**
     * Extract answer from LLM response
     */
    private String extractAnswer(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText();

                return content
                    .replaceAll("^```\\w*\\n", "")
                    .replaceAll("\\n```$", "")
                    .trim();
            }

            return "Failed to generate answer.";

        } catch (Exception e) {
            log.error("Parse LLM response failed", e);
            return "Answer parsing failed.";
        }
    }
}
