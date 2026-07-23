package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIPrivacyCleanupResultDTO {
    private Long backupId;
    private long cleanedLogCount;
    private long cleanedCommandCount;
    private LocalDateTime completedAt;
    private boolean backupRetainsSensitiveData;
}
