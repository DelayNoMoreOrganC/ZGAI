package com.lawfirm.service;

import com.lawfirm.dto.OcrHealthDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OcrServiceTest {

    private LocalDocumentTextService documentTextService;
    private OcrService service;

    @BeforeEach
    void setUp() {
        documentTextService = mock(LocalDocumentTextService.class);
        service = new OcrService(documentTextService);
        ReflectionTestUtils.setField(service, "maxFileSize", 1024L);
    }

    @Test
    void extractsThroughLocalServiceAndDeletesTemporaryFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "判决书.txt", "text/plain", "法院文书".getBytes());
        AtomicReference<Path> observedPath = new AtomicReference<>();
        when(documentTextService.extract(any(Path.class), any(), any())).thenAnswer(invocation -> {
            Path path = invocation.getArgument(0);
            observedPath.set(path);
            assertTrue(Files.exists(path));
            return "法院文书";
        });

        String result = service.recognizeDocument(file, "judgment");

        assertEquals("法院文书", result);
        assertFalse(Files.exists(observedPath.get()));
    }

    @Test
    void rejectsUnsupportedOrOversizedFilesBeforeWritingTempFile() {
        MockMultipartFile unsupported = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", new byte[]{1});
        IllegalArgumentException unsupportedError = assertThrows(IllegalArgumentException.class,
                () -> service.recognizeDocument(unsupported, "document"));
        assertEquals("仅支持 PDF、DOCX、TXT、MD、PNG、JPG 文件", unsupportedError.getMessage());

        ReflectionTestUtils.setField(service, "maxFileSize", 3L);
        MockMultipartFile oversized = new MockMultipartFile(
                "file", "notice.pdf", "application/pdf", new byte[]{1, 2, 3, 4});
        IllegalArgumentException oversizedError = assertThrows(IllegalArgumentException.class,
                () -> service.recognizeDocument(oversized, "document"));
        assertEquals("OCR文件超过系统允许大小", oversizedError.getMessage());
        verifyNoInteractions(documentTextService);
    }

    @Test
    void healthDelegatesToTheLocalExtractionStack() {
        OcrHealthDTO health = OcrHealthDTO.builder().status("READY").build();
        when(documentTextService.getHealth()).thenReturn(health);

        assertEquals(health, service.getHealth());
        verify(documentTextService).getHealth();
    }
}
