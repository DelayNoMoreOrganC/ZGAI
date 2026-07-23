package com.lawfirm.schedule;

import com.lawfirm.service.AIDocumentIntakeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIDocumentIntakeCleanupSchedule {

    private final AIDocumentIntakeService intakeService;

    @Scheduled(cron = "${ai.document-intake.cleanup-cron:0 30 * * * ?}")
    public void cleanupExpiredFiles() {
        int cleaned = intakeService.cleanupExpired(LocalDateTime.now());
        if (cleaned > 0) {
            log.info("已清理 {} 个过期待归案文件", cleaned);
        }
    }
}
