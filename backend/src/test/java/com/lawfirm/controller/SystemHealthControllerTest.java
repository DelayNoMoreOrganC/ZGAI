package com.lawfirm.controller;

import com.lawfirm.dto.OcrHealthDTO;
import com.lawfirm.service.OcrService;
import com.lawfirm.service.RAGKnowledgeService;
import com.lawfirm.service.StorageHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemHealthControllerTest {

    private DataSource dataSource;
    private StorageHealthService storageHealthService;
    private RAGKnowledgeService ragKnowledgeService;
    private OcrService ocrService;
    private SystemHealthController controller;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        Environment environment = mock(Environment.class);
        storageHealthService = mock(StorageHealthService.class);
        ragKnowledgeService = mock(RAGKnowledgeService.class);
        ocrService = mock(OcrService.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metadata.getDatabaseProductVersion()).thenReturn("16");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"postgres"});
        when(storageHealthService.getStorageStatus()).thenReturn(readyStorage());
        when(ragKnowledgeService.healthStatus()).thenReturn(Map.of("status", "READY"));
        controller = new SystemHealthController(
                dataSource, environment, storageHealthService, ragKnowledgeService, ocrService);
    }

    @Test
    void detailsIncludeOcrCapabilitiesAndAreReadyWhenAllDependenciesAreReady() {
        OcrHealthDTO ocr = readyOcr();
        when(ocrService.getHealth()).thenReturn(ocr);

        Map<String, Object> details = controller.details().getData();

        assertEquals("ready", details.get("status"));
        assertSame(ocr, details.get("ocr"));
    }

    @Test
    void detailsAreDegradedWhenScannedDocumentOcrIsUnavailable() {
        OcrHealthDTO ocr = readyOcr();
        ocr.setStatus("DEGRADED");
        ocr.setScannedPdfOcrReady(false);
        when(ocrService.getHealth()).thenReturn(ocr);

        Map<String, Object> details = controller.details().getData();

        assertEquals("degraded", details.get("status"));
        assertSame(ocr, details.get("ocr"));
    }

    private Map<String, Object> readyStorage() {
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("caseLibrary", Map.of("status", "ready"));
        storage.put("knowledgeLibrary", Map.of("status", "ready"));
        storage.put("backup", Map.of("status", "ready"));
        return storage;
    }

    private OcrHealthDTO readyOcr() {
        return OcrHealthDTO.builder()
                .status("READY")
                .textDocumentExtractionReady(true)
                .imageOcrReady(true)
                .scannedPdfOcrReady(true)
                .language("chi_sim+eng")
                .message("本地图片与扫描PDF OCR可用")
                .build();
    }
}
