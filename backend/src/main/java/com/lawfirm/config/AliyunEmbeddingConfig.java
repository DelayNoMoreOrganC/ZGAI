package com.lawfirm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.aliyun.embedding")
public class AliyunEmbeddingConfig {
    private String apiKey;
    private String baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private String model = "text-embedding-v3";
    private int dimension = 1024;
    private int timeoutSeconds = 30;
}
