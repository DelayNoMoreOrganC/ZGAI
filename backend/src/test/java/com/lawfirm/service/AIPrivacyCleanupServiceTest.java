package com.lawfirm.service;

import com.lawfirm.dto.AIPrivacyCleanupPreviewDTO;
import com.lawfirm.dto.AIPrivacyCleanupRequest;
import com.lawfirm.dto.AIPrivacyCleanupResultDTO;
import com.lawfirm.entity.AICaseCommand;
import com.lawfirm.entity.AILog;
import com.lawfirm.entity.DataBackup;
import com.lawfirm.repository.AICaseCommandRepository;
import com.lawfirm.repository.AILogRepository;
import com.lawfirm.repository.DataBackupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIPrivacyCleanupServiceTest {

    private AILogRepository logRepository;
    private AICaseCommandRepository commandRepository;
    private DataBackupRepository backupRepository;
    private BackupService backupService;
    private AIPrivacyCleanupService service;

    @BeforeEach
    void setUp() {
        logRepository = mock(AILogRepository.class);
        commandRepository = mock(AICaseCommandRepository.class);
        backupRepository = mock(DataBackupRepository.class);
        backupService = mock(BackupService.class);
        service = new AIPrivacyCleanupService(logRepository, commandRepository, backupRepository, backupService);
    }

    @Test
    void previewRequiresVerifiedBackupNewerThanSensitiveRecords() {
        LocalDateTime sensitiveAt = LocalDateTime.of(2026, 7, 23, 10, 0);
        AILog log = log(sensitiveAt);
        DataBackup oldBackup = backup(3L, sensitiveAt.minusMinutes(1));
        DataBackup eligibleBackup = backup(4L, sensitiveAt.plusMinutes(1));
        when(logRepository.countByPrivacySanitizedAtIsNull()).thenReturn(2L);
        when(commandRepository.countByPrivacySanitizedAtIsNull()).thenReturn(1L);
        when(logRepository.findFirstByPrivacySanitizedAtIsNullOrderByCreatedAtDesc())
                .thenReturn(Optional.of(log));
        when(commandRepository.findFirstByPrivacySanitizedAtIsNullOrderByCreatedAtDesc())
                .thenReturn(Optional.empty());
        when(backupRepository.findRecentBackups()).thenReturn(java.util.Arrays.asList(oldBackup, eligibleBackup));

        AIPrivacyCleanupPreviewDTO preview = service.preview();

        assertEquals("READY", preview.getStatus());
        assertEquals(3L, preview.getPendingLogCount() + preview.getPendingCommandCount());
        assertEquals(4L, preview.getEligibleBackupId());
        assertFalse(preview.getEligibleBackupFileName().contains("/private"));
    }

    @Test
    void cleanupRemovesLegacyBodiesAndKeepsHashesAndRedactedSummaries() {
        LocalDateTime sensitiveAt = LocalDateTime.of(2026, 7, 23, 10, 0);
        AILog log = log(sensitiveAt);
        log.setInputContent("客户电话13800138000，案件事实全文");
        log.setOutputContent("模型完整答复");
        log.setErrorMessage("联系client@example.com失败");
        AICaseCommand command = command(sensitiveAt.minusMinutes(1));
        command.setInstruction("记录进展，电话13800138000");
        command.setClarification("请联系client@example.com补充地点");
        DataBackup backup = backup(8L, sensitiveAt.plusMinutes(1));
        backup.setRemark("手动备份");
        when(logRepository.findAllByPrivacySanitizedAtIsNullOrderByIdAsc())
                .thenReturn(Collections.singletonList(log));
        when(commandRepository.findAllByPrivacySanitizedAtIsNullOrderByIdAsc())
                .thenReturn(Collections.singletonList(command));
        when(backupRepository.findById(8L)).thenReturn(Optional.of(backup));
        when(backupService.verifyBackup(8L)).thenReturn(true);

        AIPrivacyCleanupResultDTO result = service.cleanup(request(8L), 1L);

        assertEquals(1L, result.getCleanedLogCount());
        assertEquals(1L, result.getCleanedCommandCount());
        assertTrue(result.isBackupRetainsSensitiveData());
        assertNull(log.getInputContent());
        assertNull(log.getOutputContent());
        assertFalse(log.getInputSummary().contains("13800138000"));
        assertEquals("AI调用失败", log.getErrorMessage());
        assertEquals(64, log.getInputHash().length());
        assertEquals(64, log.getOutputHash().length());
        assertEquals("指令内容已移除（字符数：18）", command.getInstruction());
        assertFalse(command.getClarification().contains("client@example.com"));
        assertEquals(64, command.getInstructionHash().length());
        assertNotNull(log.getPrivacySanitizedAt());
        assertNotNull(command.getPrivacySanitizedAt());
        assertTrue(backup.getRemark().contains("含历史敏感原文"));
        verify(logRepository).saveAll(anyList());
        verify(commandRepository).saveAll(anyList());
        verify(backupRepository).save(backup);
    }

    @Test
    void cleanupRejectsBackupThatDoesNotCoverLatestSensitiveRecord() {
        LocalDateTime sensitiveAt = LocalDateTime.of(2026, 7, 23, 10, 0);
        when(logRepository.findAllByPrivacySanitizedAtIsNullOrderByIdAsc())
                .thenReturn(Collections.singletonList(log(sensitiveAt)));
        when(commandRepository.findAllByPrivacySanitizedAtIsNullOrderByIdAsc())
                .thenReturn(Collections.emptyList());
        when(backupRepository.findById(9L)).thenReturn(Optional.of(backup(9L, sensitiveAt.minusSeconds(1))));

        assertThrows(IllegalArgumentException.class, () -> service.cleanup(request(9L), 1L));

        verify(backupService, never()).verifyBackup(9L);
        verify(logRepository, never()).saveAll(anyList());
    }

    private AILog log(LocalDateTime createdAt) {
        AILog log = new AILog();
        log.setId(1L);
        log.setUserId(1L);
        log.setFunctionType("CASE_ANALYSIS");
        log.setCreatedAt(createdAt);
        return log;
    }

    private AICaseCommand command(LocalDateTime createdAt) {
        AICaseCommand command = new AICaseCommand();
        command.setId(2L);
        command.setUserId(1L);
        command.setIdempotencyKey("legacy-command");
        command.setStatus("AUTO_EXECUTED");
        command.setCreatedAt(createdAt);
        return command;
    }

    private DataBackup backup(Long id, LocalDateTime backupTime) {
        DataBackup backup = new DataBackup();
        backup.setId(id);
        backup.setFilePath("/private/tmp/lawfirm_backup_" + id + ".sql");
        backup.setBackupStatus("SUCCESS");
        backup.setVerificationStatus("VERIFIED");
        backup.setBackupTime(backupTime);
        backup.setDeleted(false);
        return backup;
    }

    private AIPrivacyCleanupRequest request(Long backupId) {
        AIPrivacyCleanupRequest request = new AIPrivacyCleanupRequest();
        request.setBackupId(backupId);
        request.setConfirmation(AIPrivacyCleanupService.CONFIRMATION);
        return request;
    }
}
