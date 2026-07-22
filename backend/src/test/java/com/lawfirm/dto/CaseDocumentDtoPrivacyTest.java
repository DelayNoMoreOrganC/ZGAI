package com.lawfirm.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseDocumentDtoPrivacyTest {

    @Test
    void serializedDocumentDoesNotExposeServerPath() throws Exception {
        CaseDocumentDTO document = new CaseDocumentDTO();
        document.setId(12L);
        document.setDocumentName("evidence.pdf");
        document.setFilePath("/Volumes/NAS/ZGAI/cases/private/evidence.pdf");

        String json = new ObjectMapper().writeValueAsString(document);

        assertTrue(json.contains("evidence.pdf"));
        assertFalse(json.contains("filePath"));
        assertFalse(json.contains("/Volumes/NAS"));
    }
}
