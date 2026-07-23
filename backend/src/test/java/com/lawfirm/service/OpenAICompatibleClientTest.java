package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIProviderType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAICompatibleClientTest {

    private final OpenAICompatibleClient client = new OpenAICompatibleClient(new ObjectMapper());

    @Test
    void lmStudioRequestDisablesReasoningForStableFinalContent() {
        AIConfig config = lmStudioConfig();

        Map<String, Object> body = client.buildChatRequest(config, "连接测试");

        assertEquals("qwen/qwen3.6-35b-a3b", body.get("model"));
        assertEquals("off", body.get("reasoning"));
        assertEquals(false, body.get("store"));
        assertEquals(8192, body.get("max_output_tokens"));
        assertEquals(ZgaiSystemPrompt.BASE, body.get("system_prompt"));
        assertEquals("连接测试", body.get("input"));
    }

    @Test
    void endpointPathsSupportVersionBaseAndFullChatUrl() {
        assertEquals("http://192.168.1.200:1234/v1/chat/completions",
                client.chatUrl("http://192.168.1.200:1234/v1/"));
        assertEquals("http://192.168.1.200:1234/v1/models",
                client.modelsUrl("http://192.168.1.200:1234/v1/chat/completions"));
        assertEquals("http://192.168.1.200:1234/api/v1/chat",
                client.nativeChatUrl("http://192.168.1.200:1234/v1"));
    }

    @Test
    void requestCanApplyTaskSpecificOutputBudget() {
        AIConfig config = lmStudioConfig();

        Map<String, Object> body = client.buildChatRequest(config, "知识库问答", 1536);

        assertEquals(1536, body.get("max_output_tokens"));
    }

    private AIConfig lmStudioConfig() {
        AIConfig config = new AIConfig();
        config.setProviderType(AIProviderType.LM_STUDIO.name());
        config.setModelName("qwen/qwen3.6-35b-a3b");
        config.setMaxTokens(8192);
        return config;
    }
}
