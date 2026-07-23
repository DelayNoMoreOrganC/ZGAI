package com.lawfirm.service;

import com.lawfirm.dto.KnowledgeArticleDTO;
import com.lawfirm.dto.KnowledgeArticleVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeDocumentImportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void txtImportExtractsContentAndStoresOriginalInsideKnowledgeRoot() throws Exception {
        KnowledgeArticleService articleService = mock(KnowledgeArticleService.class);
        KnowledgeArticleVO result = new KnowledgeArticleVO();
        result.setId(81L);
        when(articleService.createImportedArticle(any(KnowledgeArticleDTO.class), anyString())).thenReturn(result);
        OcrService ocrService = mock(OcrService.class);
        MockMultipartFile file = new MockMultipartFile(
                "file", "利益冲突规则.txt", "text/plain", "第一条 适用范围\n第二条 审查规则".getBytes(StandardCharsets.UTF_8));
        when(ocrService.recognizeDocument(file, "KNOWLEDGE_IMPORT"))
                .thenReturn("第一条 适用范围\n第二条 审查规则");
        KnowledgeDocumentImportService service = new KnowledgeDocumentImportService(
                articleService, ocrService, tempDir.toString(), 1024 * 1024);
        KnowledgeArticleDTO dto = dto("LAW_REGULATION");

        KnowledgeArticleVO imported = service.importDocument(file, dto);

        ArgumentCaptor<KnowledgeArticleDTO> dtoCaptor = ArgumentCaptor.forClass(KnowledgeArticleDTO.class);
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(articleService).createImportedArticle(dtoCaptor.capture(), pathCaptor.capture());
        assertEquals(81L, imported.getId());
        assertEquals("利益冲突规则", dtoCaptor.getValue().getTitle());
        assertTrue(dtoCaptor.getValue().getContent().contains("第二条 审查规则"));
        Path storedPath = Path.of(pathCaptor.getValue());
        assertTrue(storedPath.startsWith(tempDir));
        assertTrue(Files.isRegularFile(storedPath));
    }

    @Test
    void unsupportedLegacyDocIsRejectedBeforeArticleCreation() {
        KnowledgeArticleService articleService = mock(KnowledgeArticleService.class);
        KnowledgeDocumentImportService service = new KnowledgeDocumentImportService(
                articleService, mock(OcrService.class), tempDir.toString(), 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile("file", "旧制度.doc", "application/msword", new byte[]{1, 2, 3});

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.importDocument(file, dto("FIRM_POLICY")));

        assertTrue(error.getMessage().contains("PDF、DOCX、TXT、MD"));
        verify(articleService, never()).createImportedArticle(any(), anyString());
    }

    @Test
    void caseDepositCannotBeImportedIntoSharedKnowledge() {
        KnowledgeArticleService articleService = mock(KnowledgeArticleService.class);
        KnowledgeDocumentImportService service = new KnowledgeDocumentImportService(
                articleService, mock(OcrService.class), tempDir.toString(), 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile(
                "file", "案件材料.txt", "text/plain", "案件隐私内容".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> service.importDocument(file, dto("CASE_DEPOSIT")));

        verify(articleService, never()).createImportedArticle(any(), anyString());
    }

    @Test
    void scannedPdfUsesAutomaticLocalOcrBeforeArticleCreation() throws Exception {
        KnowledgeArticleService articleService = mock(KnowledgeArticleService.class);
        OcrService ocrService = mock(OcrService.class);
        KnowledgeArticleVO result = new KnowledgeArticleVO();
        result.setId(82L);
        when(articleService.createImportedArticle(any(KnowledgeArticleDTO.class), anyString())).thenReturn(result);
        MockMultipartFile file = new MockMultipartFile(
                "file", "扫描法规.pdf", "application/pdf", "%PDF scan".getBytes(StandardCharsets.UTF_8));
        when(ocrService.recognizeDocument(file, "KNOWLEDGE_IMPORT"))
                .thenReturn("第一条 扫描件由本机自动识别");
        KnowledgeDocumentImportService service = new KnowledgeDocumentImportService(
                articleService, ocrService, tempDir.toString(), 1024 * 1024);

        service.importDocument(file, dto("LAW_REGULATION"));

        verify(ocrService).recognizeDocument(file, "KNOWLEDGE_IMPORT");
        ArgumentCaptor<KnowledgeArticleDTO> captor = ArgumentCaptor.forClass(KnowledgeArticleDTO.class);
        verify(articleService).createImportedArticle(captor.capture(), anyString());
        assertTrue(captor.getValue().getContent().contains("本机自动识别"));
    }

    private KnowledgeArticleDTO dto(String source) {
        KnowledgeArticleDTO dto = new KnowledgeArticleDTO();
        dto.setKnowledgeSource(source);
        dto.setArticleType("DOCUMENT");
        dto.setIsPublic(true);
        dto.setKnowledgeEligible(true);
        return dto;
    }
}
