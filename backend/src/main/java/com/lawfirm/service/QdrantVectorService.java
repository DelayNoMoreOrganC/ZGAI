package com.lawfirm.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lawfirm.config.QdrantConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantVectorService {

    private final QdrantConfig config;
    private final Gson gson = new Gson();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * 初始化Qdrant集合
     */
    public void initializeCollection() {
        try {
            // 检查集合是否存在
            if (collectionExists()) {
                log.info("Qdrant集合 '{}' 已存在", config.getCollectionName());
                return;
            }

            // 创建集合
            createCollection();
            log.info("成功创建Qdrant集合 '{}'", config.getCollectionName());

        } catch (Exception e) {
            log.error("初始化Qdrant集合失败", e);
            throw new RuntimeException("初始化Qdrant集合失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查集合是否存在
     */
    private boolean collectionExists() {
        try {
            Request request = new Request.Builder()
                    .url(String.format("%s/collections/%s", config.getHttpUrl(), config.getCollectionName()))
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }

        } catch (Exception e) {
            log.warn("检查集合存在性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建集合
     */
    private void createCollection() {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("vector_size", config.getVectorSize());
            requestBody.addProperty("distance", config.getDistance());

            // 使用优化的索引参数
            JsonObject params = new JsonObject();
            params.addProperty("hnsw_config", buildHnswConfig());
            requestBody.add("params", params);

            Request request = new Request.Builder()
                    .url(String.format("%s/collections/%s", config.getHttpUrl(), config.getCollectionName()))
                    .put(RequestBody.create(
                            gson.toJson(requestBody),
                            MediaType.parse("application/json; charset=utf-8")
                    ))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("创建集合失败: " + response.code());
                }
            }

        } catch (Exception e) {
            log.error("创建Qdrant集合失败", e);
            throw new RuntimeException("创建集合失败: " + e.getMessage(), e);
        }
    }

    /**
     * HNSW索引配置优化
     */
    private String buildHnswConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("m", 16);  // 连接数，越大越准确但内存占用高
        config.addProperty("ef_construct", 100);  // 构建时的搜索范围
        return gson.toJson(config);
    }

    /**
     * 插入向量
     *
     * @param pointId 点ID
     * @param vector 向量
     * @param payload 元数据（如articleId, title等）
     */
    public void insertPoint(long pointId, List<Double> vector, JsonObject payload) {
        try {
            JsonObject point = new JsonObject();
            point.addProperty("id", pointId);
            point.add("vector", gson.toJsonTree(vector));
            point.add("payload", payload);

            JsonObject requestBody = new JsonObject();
            requestBody.add("points", gson.toJsonTree(List.of(point)));

            Request request = new Request.Builder()
                    .url(String.format("%s/collections/%s/points", config.getHttpUrl(), config.getCollectionName()))
                    .put(RequestBody.create(
                            gson.toJson(requestBody),
                            MediaType.parse("application/json; charset=utf-8")
                    ))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("插入向量失败: {}", response.code());
                    throw new RuntimeException("插入向量失败: " + response.code());
                }
            }

            log.debug("成功插入向量点: {}", pointId);

        } catch (Exception e) {
            log.error("插入向量失败: pointId={}", pointId, e);
            throw new RuntimeException("插入向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量插入向量（性能优化）
     *
     * @param points 点列表
     */
    public void insertPointsBatch(List<VectorPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }

        try {
            JsonArray pointsArray = new JsonArray();
            for (VectorPoint point : points) {
                JsonObject pointJson = new JsonObject();
                pointJson.addProperty("id", point.id);
                pointJson.add("vector", gson.toJsonTree(point.vector));
                pointJson.add("payload", point.payload);
                pointsArray.add(pointJson);
            }

            JsonObject requestBody = new JsonObject();
            requestBody.add("points", pointsArray);

            Request request = new Request.Builder()
                    .url(String.format("%s/collections/%s/points", config.getHttpUrl(), config.getCollectionName()))
                    .put(RequestBody.create(
                            gson.toJson(requestBody),
                            MediaType.parse("application/json; charset=utf-8")
                    ))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("批量插入向量失败: {}", response.code());
                    throw new RuntimeException("批量插入向量失败: " + response.code());
                }
            }

            log.info("成功批量插入 {} 个向量点", points.size());

        } catch (Exception e) {
            log.error("批量插入向量失败", e);
            throw new RuntimeException("批量插入向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 向量检索
     *
     * @param queryVector 查询向量
     * @param topK 返回Top K结果
     * @param scoreThreshold 相似度阈值（0-1）
     * @return 检索结果
     */
    public List<SearchResult> search(List<Double> queryVector, int topK, double scoreThreshold) {
        try {
            JsonObject searchRequest = new JsonObject();
            searchRequest.add("vector", gson.toJsonTree(queryVector));
            searchRequest.addProperty("limit", topK);
            searchRequest.addProperty("score_threshold", scoreThreshold);
            searchRequest.addProperty("with_payload", true);

            Request request = new Request.Builder()
                    .url(String.format("%s/collections/%s/points/search", config.getHttpUrl(), config.getCollectionName()))
                    .post(RequestBody.create(
                            gson.toJson(searchRequest),
                            MediaType.parse("application/json; charset=utf-8")
                    ))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("向量检索失败: {}", response.code());
                    throw new RuntimeException("向量检索失败: " + response.code());
                }

                String responseBody = response.body().string();
                return parseSearchResults(responseBody);
            }

        } catch (Exception e) {
            log.error("向量检索失败", e);
            throw new RuntimeException("向量检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除向量点
     */
    public void deletePoint(long pointId) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.add("points", gson.toJsonTree(List.of(pointId)));

            Request request = new Request.Builder()
                    .url(String.format("%s/collections/%s/points", config.getHttpUrl(), config.getCollectionName()))
                    .post(RequestBody.create(
                            gson.toJson(requestBody),
                            MediaType.parse("application/json; charset=utf-8")
                    ))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("删除向量失败: {}", response.code());
                }
            }

        } catch (Exception e) {
            log.error("删除向量失败: pointId={}", pointId, e);
        }
    }

    /**
     * 获取集合信息
     */
    public JsonObject getCollectionInfo() {
        try {
            Request request = new Request.Builder()
                    .url(String.format("%s/collections/%s", config.getHttpUrl(), config.getCollectionName()))
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }

                String responseBody = response.body().string();
                return gson.fromJson(responseBody, JsonObject.class);
            }

        } catch (Exception e) {
            log.error("获取集合信息失败", e);
            return null;
        }
    }

    private List<SearchResult> parseSearchResults(String responseBody) {
        JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
        JsonArray resultsArray = responseJson.getAsJsonArray("result");

        List<SearchResult> results = new ArrayList<>();
        for (var element : resultsArray) {
            JsonObject resultObj = element.getAsJsonObject();
            SearchResult result = new SearchResult();
            result.id = resultObj.get("id").getAsLong();
            result.score = resultObj.get("score").getAsDouble();
            result.payload = resultObj.getAsJsonObject("payload").toString();
            results.add(result);
        }

        return results;
    }

    /**
     * 向量点数据结构
     */
    public static class VectorPoint {
        public long id;
        public List<Double> vector;
        public JsonObject payload;

        public VectorPoint(long id, List<Double> vector, JsonObject payload) {
            this.id = id;
            this.vector = vector;
            this.payload = payload;
        }
    }

    /**
     * 检索结果数据结构
     */
    public static class SearchResult {
        public long id;
        public double score;
        public String payload;
    }
}
