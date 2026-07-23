package com.lawfirm.service;

import com.lawfirm.entity.DataBackup;
import com.lawfirm.repository.DataBackupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesPostgreSqlJdbcUrl() {
        BackupService.PostgreSqlConnectionInfo info = BackupService.parsePostgreSqlConnection(
                "jdbc:postgresql://192.168.1.7:5433/zgai_test?sslmode=disable");

        assertEquals("192.168.1.7", info.host);
        assertEquals(5433, info.port);
        assertEquals("zgai_test", info.database);
    }

    @Test
    void usesDefaultPostgreSqlPort() {
        BackupService.PostgreSqlConnectionInfo info = BackupService.parsePostgreSqlConnection(
                "jdbc:postgresql://localhost/zgai");

        assertEquals(5432, info.port);
    }

    @Test
    void rejectsPostgreSqlUrlWithoutDatabase() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BackupService.parsePostgreSqlConnection("jdbc:postgresql://localhost"));
    }

    @Test
    void verifiesManagedBackupAndPersistsChecksum() throws Exception {
        DataBackupRepository repository = mock(DataBackupRepository.class);
        BackupService service = service(repository);
        Path backupFile = tempDir.resolve("lawfirm_backup_test.sql");
        Files.write(backupFile, "CREATE TABLE test(id INT);".getBytes(StandardCharsets.UTF_8));
        DataBackup backup = successfulBackup(backupFile);
        when(repository.findById(8L)).thenReturn(Optional.of(backup));

        assertTrue(service.verifyBackup(8L));

        assertEquals("VERIFIED", backup.getVerificationStatus());
        assertEquals(64, backup.getContentSha256().length());
        assertTrue(backup.getVerifiedAt() != null);
        verify(repository).save(backup);
    }

    @Test
    void rejectsBackupPathOutsideConfiguredRoot() throws Exception {
        DataBackupRepository repository = mock(DataBackupRepository.class);
        BackupService service = service(repository);
        Path outside = Files.createTempFile("zgai-outside-backup", ".sql");
        DataBackup backup = successfulBackup(outside);
        when(repository.findById(9L)).thenReturn(Optional.of(backup));

        assertThrows(SecurityException.class, () -> service.verifyBackup(9L));
        verify(repository, never()).save(any());
    }

    @Test
    void failedBackupIsRecordedAndLeavesNoPartialFile() {
        DataBackupRepository repository = mock(DataBackupRepository.class);
        BackupService service = service(repository);
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:unsupported:test");

        assertThrows(RuntimeException.class, () -> service.manualBackup("test", 1L));

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(backup ->
                "FAILED".equals(backup.getBackupStatus())
                        && "FAILED".equals(backup.getVerificationStatus())));
        try (java.util.stream.Stream<Path> files = Files.list(tempDir)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".part")));
        } catch (Exception e) {
            throw new AssertionError("无法检查备份半成品", e);
        }
    }

    @Test
    void expiredOutOfRootRecordIsNotMarkedDeleted() throws Exception {
        DataBackupRepository repository = mock(DataBackupRepository.class);
        BackupService service = service(repository);
        Path outside = Files.createTempFile("zgai-outside-expired", ".sql");
        DataBackup backup = successfulBackup(outside);
        when(repository.findBackupsBeforeDate(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(backup));

        service.cleanExpiredBackups();

        assertFalse(Boolean.TRUE.equals(backup.getDeleted()));
        assertTrue(Files.exists(outside));
        verify(repository, never()).save(backup);
    }

    @Test
    void computesStableSha256() throws Exception {
        Path file = tempDir.resolve("hash.txt");
        Files.write(file, "zgai".getBytes(StandardCharsets.UTF_8));

        assertEquals("389ccd69417546a09a9346719e5d8cd41fa9acb552f686f42ce8d3c1532cbb02",
                BackupService.sha256(file));
    }

    private BackupService service(DataBackupRepository repository) {
        BackupService service = new BackupService(repository);
        ReflectionTestUtils.setField(service, "backupBaseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:h2:mem:test");
        ReflectionTestUtils.setField(service, "datasourceUsername", "sa");
        ReflectionTestUtils.setField(service, "datasourcePassword", "");
        ReflectionTestUtils.setField(service, "retentionDays", 180);
        ReflectionTestUtils.setField(service, "pgDumpPath", "pg_dump");
        ReflectionTestUtils.setField(service, "pgRestorePath", "pg_restore");
        return service;
    }

    private DataBackup successfulBackup(Path path) {
        DataBackup backup = new DataBackup();
        backup.setId(8L);
        backup.setFilePath(path.toString());
        backup.setBackupType("MANUAL");
        backup.setBackupStatus("SUCCESS");
        backup.setBackupTime(LocalDateTime.now());
        backup.setDeleted(false);
        return backup;
    }
}
