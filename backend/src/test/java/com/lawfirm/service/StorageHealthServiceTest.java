package com.lawfirm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageHealthServiceTest {

    @TempDir
    Path tempDir;

    private final StorageHealthService service = new StorageHealthService();

    @Test
    void reportsExistingWritableDirectoryWithoutExposingItsPath() {
        Map<String, Object> status = service.inspect("案件文件库", tempDir.toString());

        assertEquals("ready", status.get("status"));
        assertEquals(Boolean.TRUE, status.get("readable"));
        assertEquals(Boolean.TRUE, status.get("writable"));
        assertTrue(((Number) status.get("usableBytes")).longValue() > 0);
        assertFalse(status.containsKey("path"));
    }

    @Test
    void reportsMissingDirectoryAsMissing() {
        Map<String, Object> status = service.inspect("知识库原件", tempDir.resolve("missing").toString());

        assertEquals("missing", status.get("status"));
        assertEquals(Boolean.FALSE, status.get("exists"));
    }
}
