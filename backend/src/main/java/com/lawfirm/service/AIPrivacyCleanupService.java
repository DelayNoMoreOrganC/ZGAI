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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AIPrivacyCleanupService {

    static final String CONFIRMATION = "清理历史AI敏感原文";
    private static final String BACKUP_NOTICE = "AI隐私清理前备份，含历史敏感原文，请受控保管";

    private final AILogRepository aiLogRepository;
    private final AICaseCommandRepository commandRepository;
    private final DataBackupRepository dataBackupRepository;
    private final BackupService backupService;

    @Transactional(readOnly = true)
    public AIPrivacyCleanupPreviewDTO preview() {
        long pendingLogs = aiLogRepository.countByPrivacySanitizedAtIsNull();
        long pendingCommands = commandRepository.countByPrivacySanitizedAtIsNull();
        LocalDateTime latestSensitiveRecordAt = latestSensitiveRecordAt();
        Optional<DataBackup> eligibleBackup = latestSensitiveRecordAt == null
                ? Optional.empty()
                : findEligibleBackup(latestSensitiveRecordAt);

        AIPrivacyCleanupPreviewDTO preview = new AIPrivacyCleanupPreviewDTO();
        preview.setPendingLogCount(pendingLogs);
        preview.setPendingCommandCount(pendingCommands);
        preview.setLatestSensitiveRecordAt(latestSensitiveRecordAt);
        preview.setStatus(pendingLogs + pendingCommands == 0
                ? "CLEAN"
                : eligibleBackup.isPresent() ? "READY" : "BACKUP_REQUIRED");
        eligibleBackup.ifPresent(backup -> {
            preview.setEligibleBackupId(backup.getId());
            preview.setEligibleBackupFileName(backup.getFileName());
            preview.setEligibleBackupTime(backup.getBackupTime());
        });
        return preview;
    }

    @Transactional(rollbackFor = Exception.class)
    public AIPrivacyCleanupResultDTO cleanup(AIPrivacyCleanupRequest request, Long userId) {
        if (request == null || !CONFIRMATION.equals(request.getConfirmation())) {
            throw new IllegalArgumentException("清理确认词不正确");
        }

        List<AILog> logs = aiLogRepository.findAllByPrivacySanitizedAtIsNullOrderByIdAsc();
        List<AICaseCommand> commands = commandRepository.findAllByPrivacySanitizedAtIsNullOrderByIdAsc();
        LocalDateTime latestSensitiveRecordAt = latestSensitiveRecordAt(logs, commands);
        if (latestSensitiveRecordAt == null) {
            return result(request.getBackupId(), 0, 0, LocalDateTime.now());
        }

        DataBackup backup = dataBackupRepository.findById(request.getBackupId())
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("清理前备份不存在"));
        assertEligibleBackup(backup, latestSensitiveRecordAt);
        if (!backupService.verifyBackup(backup.getId())) {
            throw new IllegalArgumentException("清理前备份完整性校验失败");
        }

        LocalDateTime completedAt = LocalDateTime.now();
        for (AILog log : logs) sanitizeLog(log, completedAt);
        for (AICaseCommand command : commands) sanitizeCommand(command, completedAt);
        aiLogRepository.saveAll(logs);
        commandRepository.saveAll(commands);

        backup.setRemark(appendNotice(backup.getRemark()));
        dataBackupRepository.save(backup);
        return result(backup.getId(), logs.size(), commands.size(), completedAt);
    }

    private Optional<DataBackup> findEligibleBackup(LocalDateTime latestSensitiveRecordAt) {
        return dataBackupRepository.findRecentBackups().stream()
                .filter(item -> isEligibleBackup(item, latestSensitiveRecordAt))
                .max(Comparator.comparing(DataBackup::getBackupTime));
    }

    private void assertEligibleBackup(DataBackup backup, LocalDateTime latestSensitiveRecordAt) {
        if (!isEligibleBackup(backup, latestSensitiveRecordAt)) {
            throw new IllegalArgumentException("必须选择覆盖全部待清理记录的已校验成功备份");
        }
    }

    private boolean isEligibleBackup(DataBackup backup, LocalDateTime latestSensitiveRecordAt) {
        return backup != null
                && "SUCCESS".equals(backup.getBackupStatus())
                && "VERIFIED".equals(backup.getVerificationStatus())
                && backup.getBackupTime() != null
                && !backup.getBackupTime().isBefore(latestSensitiveRecordAt)
                && !Boolean.TRUE.equals(backup.getDeleted());
    }

    private LocalDateTime latestSensitiveRecordAt() {
        LocalDateTime logTime = aiLogRepository.findFirstByPrivacySanitizedAtIsNullOrderByCreatedAtDesc()
                .map(AILog::getCreatedAt).orElse(null);
        LocalDateTime commandTime = commandRepository.findFirstByPrivacySanitizedAtIsNullOrderByCreatedAtDesc()
                .map(AICaseCommand::getCreatedAt).orElse(null);
        return later(logTime, commandTime);
    }

    private LocalDateTime latestSensitiveRecordAt(List<AILog> logs, List<AICaseCommand> commands) {
        LocalDateTime result = null;
        for (AILog log : logs) result = later(result, log.getCreatedAt());
        for (AICaseCommand command : commands) result = later(result, command.getCreatedAt());
        return result;
    }

    private LocalDateTime later(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }

    private void sanitizeLog(AILog log, LocalDateTime completedAt) {
        String inputSource = StringUtils.hasText(log.getInputContent())
                ? log.getInputContent() : log.getInputSummary();
        if (!StringUtils.hasText(log.getInputHash()) && inputSource != null) {
            log.setInputHash(AIContentPrivacy.sha256(inputSource));
        }
        if (!StringUtils.hasText(log.getOutputHash()) && log.getOutputContent() != null) {
            log.setOutputHash(AIContentPrivacy.sha256(log.getOutputContent()));
        }
        log.setInputSummary(AIContentPrivacy.summarize(inputSource));
        log.setInputContent(null);
        log.setOutputContent(null);
        log.setErrorMessage(AIContentPrivacy.errorSummary(log.getErrorMessage()));
        log.setPrivacySanitizedAt(completedAt);
    }

    private void sanitizeCommand(AICaseCommand command, LocalDateTime completedAt) {
        String instruction = command.getInstruction();
        if (!StringUtils.hasText(command.getInstructionHash()) && instruction != null) {
            command.setInstructionHash(AIContentPrivacy.sha256(instruction));
        }
        command.setInstruction(AIContentPrivacy.commandSummary(instruction));
        command.setClarification(AIContentPrivacy.summarize(command.getClarification()));
        command.setPrivacySanitizedAt(completedAt);
    }

    private String appendNotice(String remark) {
        if (remark != null && remark.contains(BACKUP_NOTICE)) return remark;
        String value = StringUtils.hasText(remark) ? remark.trim() + "；" + BACKUP_NOTICE : BACKUP_NOTICE;
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private AIPrivacyCleanupResultDTO result(Long backupId, long logCount, long commandCount,
                                             LocalDateTime completedAt) {
        AIPrivacyCleanupResultDTO result = new AIPrivacyCleanupResultDTO();
        result.setBackupId(backupId);
        result.setCleanedLogCount(logCount);
        result.setCleanedCommandCount(commandCount);
        result.setCompletedAt(completedAt);
        result.setBackupRetainsSensitiveData(logCount + commandCount > 0);
        return result;
    }
}
