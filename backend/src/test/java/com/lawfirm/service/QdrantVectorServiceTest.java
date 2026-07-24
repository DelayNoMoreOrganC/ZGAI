package com.lawfirm.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lawfirm.config.QdrantConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QdrantVectorServiceTest {

    @Test
    void collectionCreationUsesCurrentVectorsSchema() {
        AtomicReference<String> putBody = new AtomicReference<>();
        OkHttpClient client = client(request -> {
            if ("GET".equals(request.method())) {
                return response(request, 404, "{}");
            }
            putBody.set(readBody(request));
            return response(request, 200, "{\"result\":true}");
        });
        QdrantVectorService service = new QdrantVectorService(config(3), client);

        service.initializeCollection();

        JsonObject sent = new Gson().fromJson(putBody.get(), JsonObject.class);
        assertEquals(3, sent.getAsJsonObject("vectors").get("size").getAsInt());
        assertEquals("Cosine", sent.getAsJsonObject("vectors").get("distance").getAsString());
        assertTrue(sent.get("hnsw_config").isJsonObject());
        assertFalse(sent.has("vector_size"));
    }

    @Test
    void healthReportsIncompatibleCollectionDimension() {
        OkHttpClient client = client(request -> response(request, 200,
                "{\"result\":{\"config\":{\"params\":{\"vectors\":{\"size\":4,\"distance\":\"Cosine\"}}}}}"));
        QdrantVectorService service = new QdrantVectorService(config(3), client);

        Map<String, Object> status = service.healthStatus();

        assertEquals("incompatible", status.get("status"));
        assertEquals(4, status.get("actualDimension"));
    }

    @Test
    void rejectsWrongDimensionBeforeSendingVector() {
        QdrantVectorService service = new QdrantVectorService(config(3), client(request -> {
            throw new AssertionError("维度校验应在发送请求前完成");
        }));

        assertThrows(IllegalArgumentException.class,
                () -> service.search(java.util.List.of(0.1, 0.2), 5, 0.6));
    }

    @Test
    void disabledVectorStoreDoesNotProbeNetwork() {
        QdrantConfig config = config(3);
        config.setEnabled(false);
        QdrantVectorService service = new QdrantVectorService(config, client(request -> {
            throw new AssertionError("停用的 Qdrant 不应发送网络请求");
        }));

        service.initializeCollection();
        service.insertPoint(1L, List.of(0.1, 0.2, 0.3), new JsonObject());
        service.insertPointsBatch(List.of(new QdrantVectorService.VectorPoint(
                1L, List.of(0.1, 0.2, 0.3), new JsonObject())));
        assertTrue(service.search(List.of(0.1, 0.2, 0.3), 3, 0.5).isEmpty());
        service.deletePoint(1L);
        assertNull(service.getCollectionInfo());
        Map<String, Object> status = service.healthStatus();

        assertEquals("disabled", status.get("status"));
    }

    private QdrantConfig config(int dimension) {
        QdrantConfig config = new QdrantConfig();
        config.setHost("127.0.0.1");
        config.setPort(6333);
        config.setCollectionName("knowledge");
        config.setVectorSize(dimension);
        config.setTimeoutSeconds(2);
        return config;
    }

    private OkHttpClient client(Responder responder) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> responder.respond(chain.request()))
                .build();
    }

    private static Response response(Request request, int code, String body) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(code >= 200 && code < 300 ? "OK" : "ERROR")
                .body(ResponseBody.create(body, MediaType.parse("application/json")))
                .build();
    }

    private static String readBody(Request request) throws IOException {
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }

    @FunctionalInterface
    private interface Responder {
        Response respond(Request request) throws IOException;
    }
}
