package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIPrivacyCleanupPreviewDTO {
    private String status;
    private long pendingLogCount;
    private long pendingCommandCount;
    private LocalDateTime latestSensitiveRecordAt;
    private Long eligibleBackupId;
    private String eligibleBackupFileName;
    private LocalDateTime eligibleBackupTime;
}
