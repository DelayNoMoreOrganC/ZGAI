package com.lawfirm.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lawfirm.config.EmbeddingConfig;
import com.lawfirm.config.QdrantConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingServiceTest {

    @Test
    void productionConstructorsAreResolvableBySpring() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(EmbeddingConfig.class);
            context.registerBean(QdrantConfig.class);
            context.register(EmbeddingService.class, QdrantVectorService.class);
            context.refresh();

            assertNotNull(context.getBean(EmbeddingService.class));
            assertNotNull(context.getBean(QdrantVectorService.class));
        }
    }

    @Test
    void localOpenAICompatibleBatchPreservesInputOrderAndChecksDimension() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        OkHttpClient client = client(request -> {
            requestBody.set(readBody(request));
            return response(request, 200, "{\"data\":["
                    + "{\"index\":1,\"embedding\":[0.4,0.5,0.6]},"
                    + "{\"index\":0,\"embedding\":[0.1,0.2,0.3]}]}");
        });
        EmbeddingService service = new EmbeddingService(localConfig(3), client);

        List<List<Double>> result = service.embedTextBatch(List.of("第一段", "第二段"));

        assertEquals(List.of(0.1, 0.2, 0.3), result.get(0));
        assertEquals(List.of(0.4, 0.5, 0.6), result.get(1));
        JsonObject sent = new Gson().fromJson(requestBody.get(), JsonObject.class);
        assertEquals("test-embedding", sent.get("model").getAsString());
        assertEquals(2, sent.getAsJsonArray("input").size());
        assertEquals("configured", service.healthStatus().get("status"));
        assertEquals(3, service.healthStatus().get("actualDimension"));
    }

    @Test
    void dimensionMismatchFailsClosedAndMarksRuntimeDegraded() {
        OkHttpClient client = client(request -> response(request, 200,
                "{\"data\":[{\"index\":0,\"embedding\":[0.1,0.2]}]}"));
        EmbeddingService service = new EmbeddingService(localConfig(3), client);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.embedQuery("诉讼时效"));

        assertTrue(error.getMessage().contains("向量维度不匹配"));
        assertEquals("degraded", service.healthStatus().get("status"));
        assertEquals(2, service.healthStatus().get("actualDimension"));
    }

    @Test
    void cloudCompatibleProviderRequiresExplicitUsableToken() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("OPENAI_COMPATIBLE");
        config.setBaseUrl("https://example.invalid/v1");
        config.setModel("cloud-embedding");
        config.setDimension(3);
        EmbeddingService service = new EmbeddingService(config);

        assertFalse(service.isConfigured());

        config.setApiKey("configured-token");
        assertTrue(service.isConfigured());
    }

    @Test
    void localProviderStillRequiresAnEmbeddingModelName() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("LM_STUDIO");
        config.setBaseUrl("http://127.0.0.1:1234/v1");
        config.setModel("");

        assertFalse(new EmbeddingService(config).isConfigured());
    }

    private EmbeddingConfig localConfig(int dimension) {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("LM_STUDIO");
        config.setBaseUrl("http://127.0.0.1:1234/v1");
        config.setModel("test-embedding");
        config.setDimension(dimension);
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
