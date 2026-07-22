package com.lawfirm.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackupServiceTest {

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
}
