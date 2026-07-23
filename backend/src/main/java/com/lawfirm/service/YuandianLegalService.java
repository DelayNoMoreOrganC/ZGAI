package com.lawfirm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.KnowledgeArticleDTO;
import com.lawfirm.dto.KnowledgeArticleVO;
import com.lawfirm.dto.YuandianSearchRequest;
import com.lawfirm.dto.YuandianSearchResponse;
import com.lawfirm.dto.YuandianSearchResultDTO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIProviderType;
import com.lawfirm.repository.AIConfigRepository;
import com.lawfirm.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class YuandianLegalService {

    private static final String PROVIDER = "YUANDIAN";
    private static final String LAW_SEARCH_ROUTE = "law_vector_search";
    private static final String CASE_SEARCH_ROUTE = "case_vector_search";
    private static final String CITATION_CHECK_ROUTE = "hall_detect";
    private static final int MAX_CONTENT_LENGTH = 30000;

    private final AIConfigRepository aiConfigRepository;
    private final KnowledgeArticleService knowledgeArticleService;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Map<String, CachedSearchResult> importCache = new ConcurrentHashMap<>();

    @Value("${yuandian.base-url:https://open.chineselaw.com}")
    private String configuredBaseUrl;

    @Value("${yuandian.api-key:}")
    private String environmentApiKey;

    @Value("${yuandian.max-results:10}")
    private int maxResults;

    @Value("${yuandian.cache-minutes:30}")
    private int cacheMinutes;

    public YuandianLegalService(
            AIConfigRepository aiConfigRepository,
            KnowledgeArticleService knowledgeArticleService,
            SecurityUtils securityUtils,
            ObjectMapper objectMapper,
            @Qualifier("yuandianRestTemplate") RestTemplate restTemplate) {
        this.aiConfigRepository = aiConfigRepository;
        this.knowledgeArticleService = knowledgeArticleService;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", PROVIDER);
        status.put("configured", StringUtils.hasText(resolveApiKey()));
        status.put("enabled", isEnabled());
        status.put("baseUrl", normalizedBaseUrl());
        status.put("maxResults", effectiveMaxResults());
        status.put("capabilities", Arrays.asList("LAW_SEARCH", "CASE_SEARCH", "CITATION_CHECK", "KNOWLEDGE_IMPORT"));
        status.put("modelAvailable", false);
        status.put("modelMessage", "元典模型仍需邀请码；当前仅启用法律数据与引证核验能力");
        return status;
    }

    public YuandianSearchResponse searchLaws(YuandianSearchRequest request) {
        int limit = normalizeLimit(request.getLimit());
        Map<String, Object> filter = new LinkedHashMap<>();
        if (!Boolean.FALSE.equals(request.getOnlyEffective())) {
            filter.put("sxx", Collections.singletonList("现行有效"));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.getQuery().trim());
        body.put("rewrite_flag", true);
        body.put("return_num", limit);
        if (!filter.isEmpty()) {
            body.put("fatiao_filter", filter);
        }

        JsonNode response = post(LAW_SEARCH_ROUTE, body);
        List<YuandianSearchResultDTO> results = new ArrayList<>();
        JsonNode items = response.path("extra").path("fatiao");
        if (items.isArray()) {
            for (JsonNode item : items) {
                if (results.size() >= limit) {
                    break;
                }
                results.add(cacheResult(toLawResult(item)));
            }
        }
        return new YuandianSearchResponse(PROVIDER, "LAW", request.getQuery().trim(), LocalDateTime.now(), results);
    }

    public YuandianSearchResponse searchCases(YuandianSearchRequest request) {
        int limit = normalizeLimit(request.getLimit());
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("dianxing", !Boolean.FALSE.equals(request.getOnlyAuthoritative()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.getQuery().trim());
        body.put("rewrite_flag", true);
        body.put("return_num", limit);
        body.put("wenshu_filter", filter);

        JsonNode response = post(CASE_SEARCH_ROUTE, body);
        List<YuandianSearchResultDTO> results = new ArrayList<>();
        JsonNode items = response.path("extra").path("wenshu");
        if (items.isArray()) {
            for (JsonNode item : items) {
                if (results.size() >= limit) {
                    break;
                }
                results.add(cacheResult(toCaseResult(item)));
            }
        }
        return new YuandianSearchResponse(PROVIDER, "CASE", request.getQuery().trim(), LocalDateTime.now(), results);
    }

    public Map<String, Object> verifyCitations(String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", content.trim());
        JsonNode response = post(CITATION_CHECK_ROUTE, body);
        return objectMapper.convertValue(response, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, String.class, Object.class));
    }

    public KnowledgeArticleVO importToKnowledge(String importToken) {
        purgeExpiredCache();
        CachedSearchResult cached = importCache.remove(importToken);
        if (cached == null || cached.expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("导入凭证已失效，请重新检索");
        }
        if (!cached.userId.equals(securityUtils.getCurrentUserId())) {
            throw new IllegalArgumentException("无权使用该导入凭证");
        }

        YuandianSearchResultDTO result = cached.result;
        KnowledgeArticleDTO article = new KnowledgeArticleDTO();
        article.setTitle(limitText(result.getTitle(), 200));
        article.setArticleType("LAW".equals(result.getResultType()) ? "DOCUMENT" : "CASE");
        article.setKnowledgeSource("LAW".equals(result.getResultType())
                ? KnowledgeArticlePolicy.LAW_REGULATION
                : KnowledgeArticlePolicy.REFERENCE_MATERIAL);
        article.setCategory(limitText(result.getCategory(), 50));
        article.setTags(limitText(joinTags("元典", result.getTags()), 500));
        article.setSummary(limitText(stripWhitespace(result.getContent()), 800));
        article.setContent(toSafeHtml(result.getContent()));
        article.setSourceReference(limitText(result.getSourceReference(), 500));
        article.setIssuingAuthority(limitText(result.getAuthority(), 200));
        article.setDocumentNumber(limitText(result.getReferenceNo(), 100));
        article.setEffectiveDate("LAW".equals(result.getResultType()) ? result.getDate() : null);
        article.setValidityStatus("LAW".equals(result.getResultType())
                ? result.getValidityStatus()
                : KnowledgeArticlePolicy.VALIDITY_UNKNOWN);
        article.setAuthorizationConfirmed("LAW".equals(result.getResultType()));
        article.setIsPublic(true);
        article.setIsTop(false);
        article.setKnowledgeEligible(Boolean.TRUE.equals(result.getRagEligible()));
        return knowledgeArticleService.createArticle(article);
    }

    public Map<String, Object> testConnection() {
        long startedAt = System.currentTimeMillis();
        YuandianSearchRequest request = new YuandianSearchRequest();
        request.setQuery("中华人民共和国民法典");
        request.setLimit(1);
        YuandianSearchResponse response = searchLaws(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("provider", PROVIDER);
        result.put("duration", System.currentTimeMillis() - startedAt);
        result.put("resultCount", response.getResults().size());
        result.put("message", "元典法规检索连接正常");
        return result;
    }

    private JsonNode post(String route, Map<String, Object> body) {
        String apiKey = requireApiKey();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-API-Key", apiKey);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizedBaseUrl() + "/open/" + route,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);
            JsonNode json = objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
            int code = json.path("code").asInt(response.getStatusCodeValue());
            if (!response.getStatusCode().is2xxSuccessful() || code < 200 || code >= 300) {
                throw new IllegalStateException("元典服务返回失败：" + responseMessage(json));
            }
            return json;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("元典服务连接失败（HTTP " + e.getRawStatusCode() + "）");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("元典服务返回了无法识别的数据");
        }
    }

    private YuandianSearchResultDTO toLawResult(JsonNode item) {
        YuandianSearchResultDTO result = new YuandianSearchResultDTO();
        result.setResultType("LAW");
        result.setExternalId(text(item, "ftid"));
        result.setParentId(text(item, "fgid"));
        String provision = text(item, "num");
        result.setTitle(joinTitle(text(item, "fgtitle"), provision));
        result.setReferenceNo(provision);
        result.setContent(limitText(text(item, "content"), MAX_CONTENT_LENGTH));
        result.setAuthority(text(item, "dy"));
        result.setCategory(text(item, "effect1"));
        result.setTags(joinTags(text(item, "effect1"), text(item, "location")));
        result.setValidityStatus(mapValidity(text(item, "sxx")));
        result.setDate(parseCompactDate(item.path("start").asText("")));
        result.setScore(item.path("score").isNumber() ? item.path("score").asDouble() : null);
        result.setSourceReference(sourceReference("law_vector_search", result.getExternalId(), result.getParentId()));
        result.setRagEligible(!KnowledgeArticlePolicy.VALIDITY_REPEALED.equals(result.getValidityStatus()));
        result.getMetadata().put("效力级别", text(item, "effect1"));
        result.getMetadata().put("地域", text(item, "location"));
        return result;
    }

    private YuandianSearchResultDTO toCaseResult(JsonNode item) {
        YuandianSearchResultDTO result = new YuandianSearchResultDTO();
        result.setResultType("CASE");
        result.setExternalId(text(item, "scid"));
        result.setTitle(text(item, "title"));
        result.setReferenceNo(text(item, "ah"));
        result.setContent(limitText(text(item, "content"), MAX_CONTENT_LENGTH));
        result.setAuthority(text(item, "jbdw"));
        result.setCategory(text(item, "ajlb"));
        result.setTags(joinTags(arrayText(item.path("ay")), arrayText(item.path("anyou")), text(item, "wszl"), text(item, "db")));
        result.setValidityStatus(KnowledgeArticlePolicy.VALIDITY_UNKNOWN);
        result.setDate(parseCompactDate(item.path("jaDate").asText("")));
        result.setScore(item.path("score").isNumber() ? item.path("score").asDouble() : null);
        result.setSourceReference(sourceReference("case_vector_search", result.getExternalId(), null));
        result.setRagEligible(false);
        result.getMetadata().put("审判程序", text(item, "spcx"));
        result.getMetadata().put("案例来源", text(item, "db"));
        result.getMetadata().put("法院层级", text(item, "cj"));
        return result;
    }

    private YuandianSearchResultDTO cacheResult(YuandianSearchResultDTO result) {
        purgeExpiredCache();
        String token = UUID.randomUUID().toString();
        result.setImportToken(token);
        importCache.put(token, new CachedSearchResult(
                securityUtils.getCurrentUserId(),
                result,
                LocalDateTime.now().plusMinutes(Math.max(5, cacheMinutes))));
        return result;
    }

    private void purgeExpiredCache() {
        LocalDateTime now = LocalDateTime.now();
        importCache.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
        if (importCache.size() > 2000) {
            importCache.clear();
        }
    }

    private boolean isEnabled() {
        return aiConfigRepository.findByProviderTypeAndDeletedFalse(AIProviderType.YUANDIAN_LEGAL.name())
                .stream()
                .anyMatch(config -> Boolean.TRUE.equals(config.getIsEnabled()));
    }

    private String resolveApiKey() {
        if (StringUtils.hasText(environmentApiKey)) {
            return environmentApiKey.trim();
        }
        return aiConfigRepository.findByProviderTypeAndDeletedFalse(AIProviderType.YUANDIAN_LEGAL.name())
                .stream()
                .filter(config -> Boolean.TRUE.equals(config.getIsEnabled()))
                .map(AIConfig::getApiKey)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse("");
    }

    private String requireApiKey() {
        if (!isEnabled()) {
            throw new IllegalStateException("元典法律数据服务未启用");
        }
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("尚未配置元典 API Key，请由管理员在系统设置中配置");
        }
        return apiKey;
    }

    private String normalizedBaseUrl() {
        String value = StringUtils.hasText(configuredBaseUrl)
                ? configuredBaseUrl.trim()
                : "https://open.chineselaw.com";
        if (!value.matches("https://open\\.chineselaw\\.com/?")) {
            throw new IllegalStateException("元典服务地址必须使用官方 HTTPS 域名");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private int normalizeLimit(Integer requested) {
        int limit = requested == null ? 8 : requested;
        return Math.max(1, Math.min(limit, effectiveMaxResults()));
    }

    private int effectiveMaxResults() {
        return Math.max(1, Math.min(maxResults, 20));
    }

    private String sourceReference(String route, String id, String parentId) {
        StringBuilder reference = new StringBuilder("https://open.chineselaw.com/api-square/")
                .append(LAW_SEARCH_ROUTE.equals(route) ? "17" : "16")
                .append(" | provider=YUANDIAN");
        if (StringUtils.hasText(id)) {
            reference.append(" | id=").append(id);
        }
        if (StringUtils.hasText(parentId)) {
            reference.append(" | parentId=").append(parentId);
        }
        return reference.toString();
    }

    private String mapValidity(String value) {
        if ("现行有效".equals(value)) {
            return KnowledgeArticlePolicy.VALIDITY_EFFECTIVE;
        }
        if ("失效".equals(value)) {
            return KnowledgeArticlePolicy.VALIDITY_REPEALED;
        }
        if ("已被修改".equals(value) || "部分失效".equals(value)) {
            return KnowledgeArticlePolicy.VALIDITY_AMENDED;
        }
        return KnowledgeArticlePolicy.VALIDITY_UNKNOWN;
    }

    private LocalDate parseCompactDate(String value) {
        if (!StringUtils.hasText(value) || "99999999".equals(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String arrayText(JsonNode node) {
        if (!node.isArray()) {
            return "";
        }
        return StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asText)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }

    private String text(JsonNode node, String field) {
        return node.path(field).isMissingNode() || node.path(field).isNull()
                ? ""
                : node.path(field).asText("").trim();
    }

    private String responseMessage(JsonNode json) {
        String message = text(json, "msg");
        return StringUtils.hasText(message) ? message : text(json, "message");
    }

    private String joinTitle(String title, String suffix) {
        if (!StringUtils.hasText(suffix) || title.endsWith(suffix)) {
            return title;
        }
        return title + " " + suffix;
    }

    private String joinTags(String... values) {
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .flatMap(value -> Arrays.stream(value.split("[,，]")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private String stripWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String toSafeHtml(String value) {
        String escaped = HtmlUtils.htmlEscape(value == null ? "" : value);
        return escaped.replace("\r\n", "<br>").replace("\n", "<br>");
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static final class CachedSearchResult {
        private final Long userId;
        private final YuandianSearchResultDTO result;
        private final LocalDateTime expiresAt;

        private CachedSearchResult(Long userId, YuandianSearchResultDTO result, LocalDateTime expiresAt) {
            this.userId = userId;
            this.result = result;
            this.expiresAt = expiresAt;
        }
    }
}
