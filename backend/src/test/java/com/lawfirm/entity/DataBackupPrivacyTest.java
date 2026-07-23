package com.lawfirm.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataBackupPrivacyTest {

    @Test
    void backupJsonShowsFileNameWithoutServerPathOrInternalError() throws Exception {
        DataBackup backup = new DataBackup();
        backup.setFilePath("/Volumes/ZGAI/backups/lawfirm_backup_20260722.sql");
        backup.setErrorMessage("internal command output");
        backup.setContentSha256("abc123");
        backup.setVerificationStatus("VERIFIED");

        String json = new ObjectMapper().writeValueAsString(backup);

        assertTrue(json.contains("lawfirm_backup_20260722.sql"));
        assertFalse(json.contains("/Volumes/ZGAI"));
        assertFalse(json.contains("internal command output"));
        assertFalse(json.contains("filePath"));
        assertFalse(json.contains("errorMessage"));
        assertTrue(json.contains("verificationStatus"));
        assertTrue(json.contains("contentSha256"));
    }
}
