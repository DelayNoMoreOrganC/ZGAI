package com.lawfirm.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lawfirm.config.EmbeddingConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmbeddingService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final EmbeddingConfig config;
    private final Gson gson = new Gson();
    private final OkHttpClient client;
    private volatile LocalDateTime lastSuccessAt;
    private volatile LocalDateTime lastFailureAt;
    private volatile String lastError;
    private volatile Long lastDurationMs;
    private volatile Integer actualDimension;

    @Autowired
    public EmbeddingService(EmbeddingConfig config) {
        this(config, buildClient(config));
    }

    EmbeddingService(EmbeddingConfig config, OkHttpClient client) {
        this.config = config;
        this.client = client;
    }

    private static OkHttpClient buildClient(EmbeddingConfig config) {
        int timeout = Math.max(1, config.getTimeoutSeconds());
        return new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    public boolean isConfigured() {
        String provider = providerType();
        if ("DISABLED".equals(provider)) {
            return false;
        }
        if (!hasText(config.getBaseUrl()) || !hasText(config.getModel()) || config.getDimension() <= 0) {
            return false;
        }
        return "LM_STUDIO".equals(provider)
                || ("OPENAI_COMPATIBLE".equals(provider) && hasUsableApiKey())
                || ("ALIYUN".equals(provider) && hasUsableApiKey());
    }

    public List<Double> embedQuery(String text) {
        return embedBatch(Collections.singletonList(text), config.getQueryPrefix()).get(0);
    }

    public List<Double> embedDocument(String text) {
        return embedBatch(Collections.singletonList(text), config.getDocumentPrefix()).get(0);
    }

    /**
     * Backward-compatible alias. New retrieval code should explicitly choose query or document mode.
     */
    public List<Double> embedText(String text) {
        return embedDocument(text);
    }

    public List<List<Double>> embedDocumentsBatch(List<String> texts) {
        return embedTextBatch(texts);
    }

    public List<List<Double>> embedTextBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        if (!isConfigured()) {
            throw new IllegalStateException("Embedding 服务未配置");
        }

        int batchSize = Math.max(1, Math.min(100, config.getMaxBatchSize()));
        List<List<Double>> embeddings = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += batchSize) {
            embeddings.addAll(embedBatch(texts.subList(i, Math.min(i + batchSize, texts.size())),
                    config.getDocumentPrefix()));
        }
        return embeddings;
    }

    private List<List<Double>> embedBatch(List<String> texts, String prefix) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        if (!isConfigured()) {
            throw new IllegalStateException("Embedding 服务未配置");
        }

        List<String> normalizedTexts = new ArrayList<>();
        for (String text : texts) {
            if (!hasText(text)) {
                throw new IllegalArgumentException("Embedding 文本不能为空");
            }
            normalizedTexts.add(normalizeInput(text, prefix));
        }

        long startedAt = System.currentTimeMillis();
        try {
            List<List<Double>> result = "ALIYUN".equals(providerType())
                    ? requestAliyun(normalizedTexts)
                    : requestOpenAICompatible(normalizedTexts);
            validateEmbeddings(result, normalizedTexts.size());
            lastSuccessAt = LocalDateTime.now();
            lastError = null;
            lastDurationMs = System.currentTimeMillis() - startedAt;
            return result;
        } catch (Exception e) {
            lastFailureAt = LocalDateTime.now();
            lastError = abbreviate(e.getMessage(), 240);
            lastDurationMs = System.currentTimeMillis() - startedAt;
            log.warn("Embedding 调用失败: provider={}, model={}, reason={}",
                    providerType(), config.getModel(), lastError);
            throw new IllegalStateException("Embedding 调用失败: " + lastError, e);
        }
    }

    private List<List<Double>> requestOpenAICompatible(List<String> texts) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.getModel());
        body.add("input", gson.toJsonTree(texts));

        Request.Builder request = new Request.Builder()
                .url(config.getEmbeddingsUrl())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON));
        if (hasText(config.getApiKey())) {
            request.addHeader("Authorization", "Bearer " + config.getApiKey().trim());
        }

        try (Response response = client.newCall(request.build()).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IllegalStateException("HTTP " + response.code() + formatRemoteError(responseBody));
            }
            JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
            JsonArray data = responseJson == null ? null : responseJson.getAsJsonArray("data");
            if (data == null) {
                throw new IllegalStateException("响应缺少 data 数组");
            }

            List<IndexedEmbedding> indexed = new ArrayList<>();
            for (int position = 0; position < data.size(); position++) {
                JsonObject item = data.get(position).getAsJsonObject();
                int index = item.has("index") ? item.get("index").getAsInt() : position;
                indexed.add(new IndexedEmbedding(index, parseVector(item.getAsJsonArray("embedding"))));
            }
            indexed.sort(Comparator.comparingInt(value -> value.index));
            List<List<Double>> result = new ArrayList<>();
            indexed.forEach(item -> result.add(item.vector));
            return result;
        }
    }

    private List<List<Double>> requestAliyun(List<String> texts) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.getModel());
        JsonObject input = new JsonObject();
        input.add("texts", gson.toJsonTree(texts));
        body.add("input", input);
        JsonObject parameters = new JsonObject();
        parameters.addProperty("text_type", "document");
        body.add("parameters", parameters);

        Request request = new Request.Builder()
                .url(config.getBaseUrl())
                .addHeader("Authorization", "Bearer " + config.getApiKey().trim())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IllegalStateException("HTTP " + response.code() + formatRemoteError(responseBody));
            }
            JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
            JsonArray data = responseJson == null || !responseJson.has("output")
                    ? null : responseJson.getAsJsonObject("output").getAsJsonArray("embeddings");
            if (data == null) {
                throw new IllegalStateException("响应缺少 output.embeddings 数组");
            }
            List<List<Double>> result = new ArrayList<>();
            for (JsonElement element : data) {
                result.add(parseVector(element.getAsJsonObject().getAsJsonArray("embedding")));
            }
            return result;
        }
    }

    private void validateEmbeddings(List<List<Double>> embeddings, int expectedCount) {
        if (embeddings == null || embeddings.size() != expectedCount) {
            throw new IllegalStateException("返回向量数量不匹配，期望 " + expectedCount
                    + "，实际 " + (embeddings == null ? 0 : embeddings.size()));
        }
        for (List<Double> embedding : embeddings) {
            int dimension = embedding == null ? 0 : embedding.size();
            actualDimension = dimension;
            if (dimension != config.getDimension()) {
                throw new IllegalStateException("向量维度不匹配，配置 " + config.getDimension()
                        + "，模型返回 " + dimension);
            }
        }
    }

    private List<Double> parseVector(JsonArray array) {
        if (array == null) {
            throw new IllegalStateException("响应缺少 embedding 向量");
        }
        List<Double> vector = new ArrayList<>(array.size());
        for (JsonElement value : array) {
            vector.add(value.getAsDouble());
        }
        return vector;
    }

    private String normalizeInput(String text, String prefix) {
        String normalized = text.replace('\u0000', ' ').trim();
        int maxChars = Math.max(256, config.getMaxInputChars());
        if (normalized.length() > maxChars) {
            normalized = normalized.substring(0, maxChars);
        }
        return hasText(prefix) ? prefix.trim() + normalized : normalized;
    }

    public Map<String, Object> healthStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        boolean configured = isConfigured();
        status.put("status", !configured ? "missing" : lastError == null ? "configured" : "degraded");
        status.put("configured", configured);
        status.put("provider", providerType());
        status.put("model", hasText(config.getModel()) ? config.getModel().trim() : "");
        status.put("configuredDimension", config.getDimension());
        status.put("actualDimension", actualDimension);
        status.put("lastSuccessAt", lastSuccessAt);
        status.put("lastFailureAt", lastFailureAt);
        status.put("lastDurationMs", lastDurationMs);
        status.put("lastError", lastError);
        return status;
    }

    public int configuredDimension() {
        return config.getDimension();
    }

    public String providerType() {
        return config.getProvider() == null ? "DISABLED"
                : config.getProvider().trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasUsableApiKey() {
        return hasText(config.getApiKey()) && !config.getApiKey().trim().startsWith("your-");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String formatRemoteError(String body) {
        return hasText(body) ? ": " + abbreviate(body.replaceAll("\\s+", " "), 200) : "";
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "未知错误";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    private static class IndexedEmbedding {
        private final int index;
        private final List<Double> vector;

        private IndexedEmbedding(int index, List<Double> vector) {
            this.index = index;
            this.vector = vector;
        }
    }
}
