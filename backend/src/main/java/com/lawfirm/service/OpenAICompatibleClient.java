package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIProviderType;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class OpenAICompatibleClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;

    @Value("${ai.lm-studio.max-concurrent:1}")
    private int maxConcurrent = 1;

    @Value("${ai.lm-studio.queue-wait-seconds:600}")
    private int queueWaitSeconds = 600;

    @Value("${ai.lm-studio.retry-count:1}")
    private int retryCount = 1;

    @Value("${ai.lm-studio.retry-delay-millis:1500}")
    private long retryDelayMillis = 1500;

    @Value("${ai.lm-studio.curl-fallback-enabled:true}")
    private boolean curlFallbackEnabled = true;

    private Semaphore lmStudioSlots = new Semaphore(1, true);

    public OpenAICompatibleClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initializeQueue() {
        lmStudioSlots = new Semaphore(Math.max(1, maxConcurrent), true);
    }

    public String chat(AIConfig config, String prompt) {
        return chat(config, prompt, null);
    }

    public String chat(AIConfig config, String prompt, Integer maxTokensOverride) {
        Map<String, Object> body = buildChatRequest(config, prompt, maxTokensOverride);

        if (!AIProviderType.LM_STUDIO.name().equals(config.getProviderType())) {
            return executeChat(config, body);
        }

        boolean acquired = false;
        try {
            acquired = lmStudioSlots.tryAcquire(Math.max(30, queueWaitSeconds), TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("本地AI任务排队超时，请稍后重试");
            }
            return executeWithRetry(config, body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("本地AI任务已取消", e);
        } finally {
            if (acquired) {
                lmStudioSlots.release();
            }
        }
    }

    private String executeWithRetry(AIConfig config, Map<String, Object> body) {
        int attempts = Math.max(0, retryCount) + 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return executeChat(config, body);
            } catch (LocalModelHttpException e) {
                int status = e.statusCode;
                boolean retryable = status == 502 || status == 503 || status == 504;
                log.warn("LM Studio请求失败: status={}, attempt={}/{}, retryable={}, reason={}",
                        status, attempt, attempts, retryable, e.getMessage());
                if (!retryable || attempt == attempts) {
                    throw e;
                }
                sleepBeforeRetry();
            }
        }
        throw new IllegalStateException("本地AI服务调用失败");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(Math.max(100, retryDelayMillis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("本地AI任务已取消", e);
        }
    }

    private String executeChat(AIConfig config, Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            Object outputBudget = body.containsKey("max_output_tokens")
                    ? body.get("max_output_tokens") : body.get("max_tokens");
            log.info("LM Studio生成请求: requestChars={}, maxTokens={}", json.length(), outputBudget);
            String url = AIProviderType.LM_STUDIO.name().equals(config.getProviderType())
                    ? nativeChatUrl(config.getApiUrl()) : chatUrl(config.getApiUrl());
            Request request = requestBuilder(config, url)
                    .post(RequestBody.create(json, JSON))
                    .build();
            String responseBody;
            try {
                responseBody = execute(config, request);
            } catch (IllegalStateException e) {
                if (!curlFallbackEnabled
                        || !AIProviderType.LM_STUDIO.name().equals(config.getProviderType())
                        || !hasCause(e, IOException.class)) {
                    throw e;
                }
                log.warn("LM Studio Java直连被对端中断，切换本机安全兼容通道");
                responseBody = executeWithCurl(config, url, json);
            }
            String content = AIProviderType.LM_STUDIO.name().equals(config.getProviderType())
                    ? parseNativeContent(responseBody)
                    : parseMessageContent(responseBody);
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("模型未返回正文，请检查推理模式和 Max Tokens 配置");
            }
            return content;
        } catch (IllegalStateException | LocalModelHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("无法解析本地模型响应", e);
        }
    }

    private String parseMessageContent(String responseBody) throws Exception {
        return objectMapper.readTree(responseBody)
                .path("choices").path(0).path("message").path("content")
                .asText("").trim();
    }

    private String parseNativeContent(String responseBody) throws Exception {
        StringBuilder content = new StringBuilder();
        JsonNode output = objectMapper.readTree(responseBody).path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                if ("message".equals(item.path("type").asText())) {
                    if (content.length() > 0) {
                        content.append('\n');
                    }
                    content.append(item.path("content").asText(""));
                }
            }
        }
        return content.toString().trim();
    }

    private String executeWithCurl(AIConfig config, String url, String json) {
        Path headerFile = null;
        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add("curl");
            command.add("--silent");
            command.add("--show-error");
            command.add("--fail-with-body");
            command.add("--max-time");
            command.add(String.valueOf(boundedTimeoutSeconds(config)));
            command.add("--header");
            command.add("Content-Type: application/json; charset=utf-8");
            command.add("--header");
            command.add("Accept: application/json");
            if (StringUtils.hasText(config.getApiKey())) {
                headerFile = Files.createTempFile("zgai-lm-auth-", ".header");
                try {
                    Files.setPosixFilePermissions(headerFile, PosixFilePermissions.fromString("rw-------"));
                } catch (UnsupportedOperationException ignored) {
                    // The default temp-file permissions are already owner-only on supported ZGAI hosts.
                }
                Files.writeString(headerFile, "Authorization: Bearer " + config.getApiKey().trim(),
                        StandardCharsets.UTF_8);
                command.add("--header");
                command.add("@" + headerFile.toAbsolutePath());
            }
            command.add("--data-binary");
            command.add("@-");
            command.add(url);

            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(json.getBytes(StandardCharsets.UTF_8));
            }
            String response = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("本地模型兼容通道调用失败，退出码 " + exitCode);
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("本地AI任务已取消", e);
        } catch (IOException e) {
            throw new IllegalStateException("无法启动本地模型兼容通道", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (headerFile != null) {
                try {
                    Files.deleteIfExists(headerFile);
                } catch (IOException e) {
                    log.warn("本地模型临时鉴权文件清理失败: {}", headerFile.getFileName());
                }
            }
        }
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> causeType) {
        Throwable current = error;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    Map<String, Object> buildChatRequest(AIConfig config, String prompt) {
        return buildChatRequest(config, prompt, null);
    }

    Map<String, Object> buildChatRequest(AIConfig config, String prompt, Integer maxTokensOverride) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", requireText(config.getModelName(), "模型名称未配置"));
        int configuredMaxTokens = config.getMaxTokens() == null ? 4096 : config.getMaxTokens();
        int outputBudget = maxTokensOverride == null
                ? configuredMaxTokens
                : Math.max(256, Math.min(maxTokensOverride, configuredMaxTokens));
        if (AIProviderType.LM_STUDIO.name().equals(config.getProviderType())) {
            body.put("system_prompt", ZgaiSystemPrompt.BASE);
            body.put("input", prompt);
            body.put("temperature", config.getTemperature() == null ? 0.1 : config.getTemperature());
            body.put("max_output_tokens", outputBudget);
            body.put("reasoning", "off");
            body.put("store", false);
        } else {
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            body.put("temperature", config.getTemperature() == null ? 0.1 : config.getTemperature());
            body.put("max_tokens", outputBudget);
        }
        return body;
    }

    public Map<String, Object> testConnection(AIConfig config) {
        long startedAt = System.currentTimeMillis();
        try {
            Request request = requestBuilder(config, modelsUrl(config.getApiUrl())).get().build();
            JsonNode data = objectMapper.readTree(execute(config, request)).path("data");
            List<String> models = data.isArray()
                    ? StreamSupport.stream(data.spliterator(), false)
                    .map(item -> item.path("id").asText(""))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList())
                    : List.of();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", models.contains(config.getModelName()) ? "ok" : "error");
            result.put("provider", config.getProviderType());
            result.put("model", config.getModelName());
            result.put("models", models);
            result.put("duration", System.currentTimeMillis() - startedAt);
            result.put("message", models.contains(config.getModelName())
                    ? "LM Studio连接正常，目标模型可用"
                    : "LM Studio已连接，但目标模型不在服务列表中");
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("无法解析LM Studio模型列表", e);
        }
    }

    private String execute(AIConfig config, Request request) {
        try (Response response = httpClient(config).newCall(request).execute()) {
            String body = response.body() == null ? "{}" : response.body().string();
            if (!response.isSuccessful()) {
                String errorSummary = summarizeErrorBody(body);
                throw new LocalModelHttpException(response.code(), "本地模型服务返回HTTP " + response.code()
                        + (StringUtils.hasText(errorSummary) ? "：" + errorSummary : ""));
            }
            return body;
        } catch (LocalModelHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("无法连接本地模型服务", e);
        }
    }

    private OkHttpClient httpClient(AIConfig config) {
        int boundedTimeout = boundedTimeoutSeconds(config);
        return new OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(boundedTimeout, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private int boundedTimeoutSeconds(AIConfig config) {
        int timeoutSeconds = config.getTimeoutSeconds() == null ? 180 : config.getTimeoutSeconds();
        return Math.max(30, Math.min(timeoutSeconds, 600));
    }

    private Request.Builder requestBuilder(AIConfig config, String url) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Accept-Encoding", "identity")
                .header("Connection", "close");
        if (StringUtils.hasText(config.getApiKey())) {
            builder.header("Authorization", "Bearer " + config.getApiKey().trim());
        }
        return builder;
    }

    private String summarizeErrorBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }
        String summary = body.replaceAll("[\\r\\n\\t]+", " ").trim();
        return summary.length() > 300 ? summary.substring(0, 300) : summary;
    }

    String chatUrl(String apiUrl) {
        String base = normalizedBaseUrl(apiUrl);
        return base.endsWith("/chat/completions") ? base : base + "/chat/completions";
    }

    String nativeChatUrl(String apiUrl) {
        String base = normalizedBaseUrl(apiUrl);
        int versionIndex = base.indexOf("/v1");
        String origin = versionIndex >= 0 ? base.substring(0, versionIndex) : base;
        return origin + "/api/v1/chat";
    }

    String modelsUrl(String apiUrl) {
        String base = normalizedBaseUrl(apiUrl);
        if (base.endsWith("/chat/completions")) {
            return base.substring(0, base.length() - "/chat/completions".length()) + "/models";
        }
        return base + "/models";
    }

    private String normalizedBaseUrl(String apiUrl) {
        String value = requireText(apiUrl, "模型服务地址未配置").replaceAll("/+$", "");
        URI uri = URI.create(value);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || !StringUtils.hasText(uri.getHost()) || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("模型服务地址格式不正确");
        }
        return value;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static final class LocalModelHttpException extends RuntimeException {
        private final int statusCode;

        private LocalModelHttpException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
