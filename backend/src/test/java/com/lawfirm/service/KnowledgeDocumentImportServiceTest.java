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
    void txtImportExtractsContentAndStoresOriginalInsideKnowledgeRoot() {
        KnowledgeArticleService articleService = mock(KnowledgeArticleService.class);
        KnowledgeArticleVO result = new KnowledgeArticleVO();
        result.setId(81L);
        when(articleService.createImportedArticle(any(KnowledgeArticleDTO.class), anyString())).thenReturn(result);
        KnowledgeDocumentImportService service = new KnowledgeDocumentImportService(articleService, tempDir.toString(), 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile(
                "file", "利益冲突规则.txt", "text/plain", "第一条 适用范围\n第二条 审查规则".getBytes(StandardCharsets.UTF_8));
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
        KnowledgeDocumentImportService service = new KnowledgeDocumentImportService(articleService, tempDir.toString(), 1024 * 1024);
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
        KnowledgeDocumentImportService service = new KnowledgeDocumentImportService(articleService, tempDir.toString(), 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile(
                "file", "案件材料.txt", "text/plain", "案件隐私内容".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> service.importDocument(file, dto("CASE_DEPOSIT")));

        verify(articleService, never()).createImportedArticle(any(), anyString());
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
