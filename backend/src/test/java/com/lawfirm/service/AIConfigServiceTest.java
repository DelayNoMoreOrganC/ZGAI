package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.dto.AIConfigVO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.repository.AIConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIConfigServiceTest {

    @Test
    void blankSecretOnUpdatePreservesExistingApiKey() {
        AIConfigRepository repository = mock(AIConfigRepository.class);
        AIConfigService service = new AIConfigService(repository);
        AIConfig existing = config("stored-secret");
        existing.setId(12L);
        when(repository.findById(12L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        AIConfigDTO update = new AIConfigDTO();
        update.setConfigName("DeepSeek");
        update.setProviderType("DEEPSEEK_API");
        update.setApiKey("  ");

        AIConfig saved = service.updateConfig(12L, update);

        assertEquals("stored-secret", saved.getApiKey());
        verify(repository).save(existing);
    }

    @Test
    void responseViewReportsSecretStateWithoutSerializingSecret() throws Exception {
        AIConfigVO view = AIConfigVO.from(config("stored-secret"));

        JsonNode json = new ObjectMapper().valueToTree(view);

        assertTrue(json.get("apiKeyConfigured").asBoolean());
        assertFalse(json.has("apiKey"));
        assertFalse(json.toString().contains("stored-secret"));
    }

    private AIConfig config(String apiKey) {
        AIConfig config = new AIConfig();
        config.setConfigName("DeepSeek");
        config.setProviderType("DEEPSEEK_API");
        config.setApiKey(apiKey);
        return config;
    }
}
