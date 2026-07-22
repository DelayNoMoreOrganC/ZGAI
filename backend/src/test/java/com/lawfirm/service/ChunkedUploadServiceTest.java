package com.lawfirm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkedUploadServiceTest {

    @TempDir
    Path tempDir;

    private ChunkedUploadService service;

    @BeforeEach
    void setUp() {
        service = new ChunkedUploadService();
        ReflectionTestUtils.setField(service, "uploadTempDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "chunkSize", 5L);
        ReflectionTestUtils.setField(service, "maxFileSize", 10L);
    }

    @Test
    void rejectsFilesOverConfiguredLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> service.initChunkedUpload("evidence.pdf", 11L, "application/pdf", 7L));
    }

    @Test
    void uploadSessionIsOwnedByItsCreator() {
        String uploadId = service.initChunkedUpload("evidence.pdf", 5L, "application/pdf", 7L);

        assertThrows(AccessDeniedException.class,
                () -> service.getUploadProgress(uploadId, 8L, false));
        assertEquals(uploadId, service.getUploadProgress(uploadId, 8L, true).getUploadId());
    }

    @Test
    void duplicateChunkRetryDoesNotInflateProgress() {
        String uploadId = service.initChunkedUpload("evidence.pdf", 5L, "application/pdf", 7L);
        MockMultipartFile chunk = new MockMultipartFile("chunk", new byte[] {1, 2, 3, 4, 5});

        service.uploadChunk(uploadId, 0, chunk, 7L, false);
        ChunkedUploadService.UploadProgress progress = service.uploadChunk(uploadId, 0, chunk, 7L, false);

        assertEquals(1, progress.getUploadedChunks());
        assertEquals(5L, progress.getUploadedBytes());
    }

    @Test
    void mergesOnlyCompleteUploadsAndKeepsServerPathPrivate() {
        String uploadId = service.initChunkedUpload("evidence.pdf", 6L, "application/pdf", 7L);
        service.uploadChunk(uploadId, 0,
                new MockMultipartFile("chunk", new byte[] {1, 2, 3, 4, 5}), 7L, false);
        assertThrows(IllegalArgumentException.class,
                () -> service.mergeChunks(uploadId, 7L, false));

        service.uploadChunk(uploadId, 1,
                new MockMultipartFile("chunk", new byte[] {6}), 7L, false);
        String path = service.mergeChunks(uploadId, 7L, false);

        assertTrue(java.nio.file.Files.exists(Path.of(path)));
        assertEquals("COMPLETED", service.getUploadProgress(uploadId, 7L, false).getStatus());
    }
}
