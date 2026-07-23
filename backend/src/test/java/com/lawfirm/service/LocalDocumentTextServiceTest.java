package com.lawfirm.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentTextServiceTest {
    @Test
    void extractsUtf8TextWithoutInvokingOcr(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("判决书.txt");
        Files.writeString(file, "广东省测试人民法院\n民事判决书", StandardCharsets.UTF_8);

        String text = new LocalDocumentTextService().extract(file, file.getFileName().toString(), "text/plain");

        assertEquals("广东省测试人民法院\n民事判决书", text);
    }

    @Test
    void extractsTextLayerFromPdfWithoutOcr(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("notice.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 18);
                stream.newLineAtOffset(72, 700);
                stream.showText("COURT NOTICE 2026 - TEST DOCUMENT WITH TEXT LAYER FOR LOCAL EXTRACTION WITHOUT OCR FALLBACK");
                stream.endText();
            }
            document.save(file.toFile());
        }

        String text = new LocalDocumentTextService().extract(file, "notice.pdf", "application/pdf");

        assertTrue(text.contains("COURT NOTICE 2026"));
    }

    @Test
    void rejectsPdfBeyondConfiguredPageLimitBeforeRendering(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("oversized-pages.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            document.save(file.toFile());
        }
        LocalDocumentTextService service = new LocalDocumentTextService();
        ReflectionTestUtils.setField(service, "maxPdfPages", 1);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.extract(file, "oversized-pages.pdf", "application/pdf"));

        assertEquals("扫描PDF不能超过1页", error.getMessage());
    }

    @Test
    void healthReportsRealCommandAndLanguageReadiness(@TempDir Path tempDir) throws Exception {
        Path probe = tempDir.resolve("ocr-probe.sh");
        Files.writeString(probe,
                "#!/bin/sh\nprintf 'List of available languages\\nchi_sim\\neng\\n'\n",
                StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(probe, PosixFilePermissions.fromString("rwx------"));
        LocalDocumentTextService service = new LocalDocumentTextService();
        ReflectionTestUtils.setField(service, "tesseractCommand", probe.toString());
        ReflectionTestUtils.setField(service, "pdftoppmCommand", probe.toString());
        ReflectionTestUtils.setField(service, "ocrLanguage", "chi_sim+eng");

        var health = service.getHealth();

        assertEquals("READY", health.getStatus());
        assertTrue(health.isImageOcrReady());
        assertTrue(health.isScannedPdfOcrReady());
    }

    @Test
    void healthIsDegradedWhenOcrCommandsAreUnavailable() {
        LocalDocumentTextService service = new LocalDocumentTextService();
        ReflectionTestUtils.setField(service, "tesseractCommand", "/missing/tesseract");
        ReflectionTestUtils.setField(service, "pdftoppmCommand", "/missing/pdftoppm");
        ReflectionTestUtils.setField(service, "ocrLanguage", "chi_sim+eng");

        var health = service.getHealth();

        assertEquals("DEGRADED", health.getStatus());
        assertTrue(health.isTextDocumentExtractionReady());
        assertFalse(health.isImageOcrReady());
        assertFalse(health.isScannedPdfOcrReady());
    }
}
