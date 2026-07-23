package com.lawfirm.service;

import com.lawfirm.entity.AILog;
import com.lawfirm.enums.AIFunctionType;
import com.lawfirm.repository.AILogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AILogServiceTest {

    @Test
    void newLogStoresOnlyRedactedSummaryAndHashes() {
        AILogRepository repository = mock(AILogRepository.class);
        AILogService service = new AILogService(repository);

        service.log(1L, 2L, AIFunctionType.CASE_ANALYSIS,
                "客户手机号13800138000，固话020-12345678，证件440101199001011234，"
                        + "邮箱client@example.com，信用代码91440101MA5ABCDEF1，银行卡6222021234567890123", 20,
                "完整案件答复", 10, "LM_STUDIO", "qwen", "SUCCESS", 90, null, 0L);

        ArgumentCaptor<AILog> captor = ArgumentCaptor.forClass(AILog.class);
        verify(repository).save(captor.capture());
        AILog saved = captor.getValue();
        assertNull(saved.getInputContent());
        assertNull(saved.getOutputContent());
        assertFalse(saved.getInputSummary().contains("13800138000"));
        assertFalse(saved.getInputSummary().contains("020-12345678"));
        assertFalse(saved.getInputSummary().contains("440101199001011234"));
        assertFalse(saved.getInputSummary().contains("client@example.com"));
        assertFalse(saved.getInputSummary().contains("91440101MA5ABCDEF1"));
        assertFalse(saved.getInputSummary().contains("6222021234567890123"));
        assertEquals(64, saved.getInputHash().length());
        assertEquals(64, saved.getOutputHash().length());
        assertEquals("LM_STUDIO", saved.getProviderType());
        assertNotNull(saved.getPrivacySanitizedAt());
    }

    @Test
    void newLogNeverPersistsRawErrorMessage() {
        AILogRepository repository = mock(AILogRepository.class);
        AILogService service = new AILogService(repository);

        service.log(1L, 2L, AIFunctionType.CASE_ANALYSIS,
                "问题", 2, null, 0, "LM_STUDIO", "qwen", "FAILED", 30,
                "上游返回客户client@example.com的完整材料", 0L);

        ArgumentCaptor<AILog> captor = ArgumentCaptor.forClass(AILog.class);
        verify(repository).save(captor.capture());
        assertEquals("AI调用失败", captor.getValue().getErrorMessage());
    }
}
