package com.lawfirm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.embedding")
public class EmbeddingConfig {

    private String provider = "LM_STUDIO";
    private String baseUrl = "http://localhost:1234/v1";
    private String apiKey = "";
    private String model = "";
    private int dimension = 1024;
    private int timeoutSeconds = 30;
    private int maxBatchSize = 25;
    private int maxInputChars = 8000;
    private String queryPrefix = "";
    private String documentPrefix = "";

    public String getEmbeddingsUrl() {
        String normalized = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        if (normalized.endsWith("/embeddings")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/embeddings";
        }
        return normalized + "/v1/embeddings";
    }
}
