package com.lawfirm.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lawfirm.config.AliyunEmbeddingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final AliyunEmbeddingConfig config;
    private final Gson gson = new Gson();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 生成文本的向量表示
     *
     * @param text 输入文本
     * @return 向量数组（1024维）
     */
    public List<Double> embedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本不能为空");
        }

        try {
            // 构建请求体
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getModel());
            requestBody.addProperty("input", buildInput(text));
            requestBody.add("parameters", buildParameters());

            // 创建HTTP请求
            Request request = new Request.Builder()
                    .url(config.getBaseUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            requestBody.toString(),
                            MediaType.parse("application/json; charset=utf-8")
                    ))
                    .build();

            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("阿里云Embedding API调用失败: {}", response.code());
                    throw new RuntimeException("Embedding API调用失败: " + response.code());
                }

                String responseBody = response.body().string();
                return parseEmbeddingResponse(responseBody);
            }

        } catch (Exception e) {
            log.error("生成文本向量失败", e);
            throw new RuntimeException("生成文本向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量生成向量（优化性能）
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    public List<List<Double>> embedTextBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        // 阿里云API支持批量请求，最多25条
        int batchSize = 25;
        List<List<Double>> embeddings = new java.util.ArrayList<>();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            embeddings.addAll(embedTextBatchInternal(batch));
        }

        return embeddings;
    }

    private List<List<Double>> embedTextBatchInternal(List<String> texts) {
        try {
            // 构建批量请求
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getModel());
            requestBody.addProperty("input", buildBatchInput(texts));
            requestBody.add("parameters", buildParameters());

            Request request = new Request.Builder()
                    .url(config.getBaseUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            requestBody.toString(),
                            MediaType.parse("application/json; charset=utf-8")
                    ))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("批量Embedding调用失败: {}", response.code());
                    throw new RuntimeException("批量Embedding调用失败: " + response.code());
                }

                String responseBody = response.body().string();
                return parseBatchEmbeddingResponse(responseBody);
            }

        } catch (Exception e) {
            log.error("批量生成向量失败", e);
            throw new RuntimeException("批量生成向量失败: " + e.getMessage(), e);
        }
    }

    private String buildInput(String text) {
        // 阿里云API格式：文本需要截断到2048字符
        if (text.length() > 2048) {
            text = text.substring(0, 2048);
        }
        return text;
    }

    private String buildBatchInput(List<String> texts) {
        JsonObject inputObj = new JsonObject();
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text.length() > 2048) {
                text = text.substring(0, 2048);
            }
            inputObj.addProperty(String.valueOf(i), text);
        }
        return gson.toJson(inputObj);
    }

    private JsonObject buildParameters() {
        JsonObject params = new JsonObject();
        params.addProperty("text_type", "document"); // 或 "query"
        return params;
    }

    private List<Double> parseEmbeddingResponse(String responseBody) {
        JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
        return gson.fromJson(
                responseJson
                        .getAsJsonObject("output")
                        .getAsJsonArray("embeddings")
                        .get(0)
                        .getAsJsonObject()
                        .get("embedding"),
                List.class
        );
    }

    private List<List<Double>> parseBatchEmbeddingResponse(String responseBody) {
        JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
        var embeddingsArray = responseJson
                .getAsJsonObject("output")
                .getAsJsonArray("embeddings");

        List<List<Double>> embeddings = new java.util.ArrayList<>();
        for (var element : embeddingsArray) {
            List<Double> embedding = gson.fromJson(
                    element.getAsJsonObject().get("embedding"),
                    List.class
            );
            embeddings.add(embedding);
        }

        return embeddings;
    }
}
