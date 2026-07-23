package com.lawfirm.enums;

public enum ArchiveJobStatus {
    PRECHECK,
    OCR,
    CLASSIFYING,
    EXTRACTING,
    LAWYER_REVIEW,
    ADMIN_REVIEW,
    ASSEMBLING,
    COMPLETED,
    FAILED,
    REJECTED;

    public boolean isActive() {
        return this != COMPLETED && this != FAILED && this != REJECTED;
    }
}
