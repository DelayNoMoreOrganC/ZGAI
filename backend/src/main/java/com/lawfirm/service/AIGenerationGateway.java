package com.lawfirm.service;

import com.lawfirm.entity.AIConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIGenerationGateway {

    private final AIConfigService aiConfigService;
    private final OpenAICompatibleClient client;

    public GenerationResult generate(String requestedProvider, String prompt) {
        return generate(requestedProvider, prompt, null);
    }

    public GenerationResult generate(String requestedProvider, String prompt, Integer maxTokens) {
        AIConfig config = aiConfigService.resolveGenerationConfig(requestedProvider);
        String content = client.chat(config, prompt, maxTokens);
        return new GenerationResult(content, config.getProviderType(), config.getModelName());
    }

    public GenerationResult generateLocally(String prompt, Integer maxTokens) {
        return generate("LM_STUDIO", prompt, maxTokens);
    }

    @Getter
    @AllArgsConstructor
    public static class GenerationResult {
        private final String content;
        private final String providerType;
        private final String modelName;
    }
}
