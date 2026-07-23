package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.dto.AIConfigVO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.repository.AIConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;

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
        AIConfigService service = new AIConfigService(
                repository,
                mock(YuandianLegalService.class),
                mock(OpenAICompatibleClient.class));
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

    @Test
    void emptySelectionAlwaysResolvesEnabledLmStudioInsteadOfCloudDefault() {
        AIConfigRepository repository = mock(AIConfigRepository.class);
        AIConfigService service = new AIConfigService(repository, mock(YuandianLegalService.class),
                mock(OpenAICompatibleClient.class));
        AIConfig local = config("");
        local.setProviderType("LM_STUDIO");
        local.setApiUrl("http://192.168.1.200:1234/v1");
        local.setIsEnabled(true);
        when(repository.findFirstByProviderTypeAndIsEnabledTrueAndDeletedFalseOrderByIsDefaultDescIdAsc("LM_STUDIO"))
                .thenReturn(Optional.of(local));

        assertEquals(local, service.resolveGenerationConfig(null));
    }

    @Test
    void documentAnalysisAlwaysResolvesLmStudio() {
        AIConfigRepository repository = mock(AIConfigRepository.class);
        AIConfigService service = new AIConfigService(repository, mock(YuandianLegalService.class),
                mock(OpenAICompatibleClient.class));
        AIConfig local = config("");
        local.setProviderType("LM_STUDIO");
        local.setApiUrl("http://192.168.1.200:1234/v1");
        local.setIsEnabled(true);
        when(repository.findFirstByProviderTypeAndIsEnabledTrueAndDeletedFalseOrderByIsDefaultDescIdAsc("LM_STUDIO"))
                .thenReturn(Optional.of(local));

        assertEquals(local, service.getUsableLocalDocumentConfigOrNull());
    }

    @Test
    void unavailableCloudProviderIsListedWithoutExposingSecret() {
        AIConfigRepository repository = mock(AIConfigRepository.class);
        AIConfigService service = new AIConfigService(repository, mock(YuandianLegalService.class),
                mock(OpenAICompatibleClient.class));
        AIConfig cloud = config("");
        cloud.setProviderType("KIMI_API");
        cloud.setApiUrl("https://api.moonshot.cn/v1");
        cloud.setModelName("kimi-model");
        cloud.setIsEnabled(true);
        when(repository.findByDeletedFalse()).thenReturn(List.of(cloud));

        assertFalse(service.getAvailableProviders().get(0).isAvailable());
        assertFalse(new ObjectMapper().valueToTree(service.getAvailableProviders()).toString().contains("apiKey"));
    }

    private AIConfig config(String apiKey) {
        AIConfig config = new AIConfig();
        config.setConfigName("DeepSeek");
        config.setProviderType("DEEPSEEK_API");
        config.setApiKey(apiKey);
        return config;
    }
}
