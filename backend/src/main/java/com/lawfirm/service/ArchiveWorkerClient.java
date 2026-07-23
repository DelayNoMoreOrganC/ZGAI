package com.lawfirm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ArchiveWorkerClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;

    @Value("${archive.worker.base-url:http://127.0.0.1:8091}")
    private String baseUrl;

    @Value("${archive.worker.token:}")
    private String token;

    @Value("${archive.worker.timeout-seconds:1800}")
    private int timeoutSeconds;

    public Map<String, Object> analyze(Map<String, Object> payload) {
        return post("/internal/archive/analyze", payload);
    }

    public Map<String, Object> assemble(Map<String, Object> payload) {
        return post("/internal/archive/assemble", payload);
    }

    private Map<String, Object> post(String path, Map<String, Object> payload) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("归档Worker令牌尚未配置");
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(Math.max(60, timeoutSeconds)))
                .writeTimeout(Duration.ofSeconds(Math.max(60, timeoutSeconds)))
                .build();
        try {
            String json = objectMapper.writeValueAsString(payload);
            Request request = new Request.Builder()
                    .url(normalizeBaseUrl() + path)
                    .header("Authorization", "Bearer " + token.trim())
                    .post(RequestBody.create(json, JSON))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("归档Worker调用失败(" + response.code() + ")");
                }
                return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() { });
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("无法连接本地归档Worker", e);
        }
    }

    private String normalizeBaseUrl() {
        String value = baseUrl == null ? "" : baseUrl.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
