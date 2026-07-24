package com.lawfirm.service;

import com.lawfirm.dto.RagEvaluationImportResult;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.entity.RagEvaluationCase;
import com.lawfirm.repository.KnowledgeArticleRepository;
import com.lawfirm.repository.RagEvaluationCaseRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RagEvaluationWorkbookServiceTest {

    @Test
    void templateContainsSampleAndDocumentSheetsWithoutArticleBody() throws Exception {
        KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
        KnowledgeArticle article = article(7L, true);
        article.setTitle("劳动争议调解仲裁法");
        article.setContent("不应写入模板的正文");
        when(articleRepository.findAll()).thenReturn(List.of(article));
        RagEvaluationWorkbookService service = service(mock(RagEvaluationCaseRepository.class),
                articleRepository, mock(RagEvaluationService.class));

        byte[] template = service.createTemplate();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(template))) {
            assertNotNull(workbook.getSheet("评价样本"));
            assertNotNull(workbook.getSheet("文档清单"));
            assertEquals("劳动争议调解仲裁法",
                    workbook.getSheet("文档清单").getRow(1).getCell(1).getStringCellValue());
            assertFalse(new String(template).contains("不应写入模板的正文"));
        }
    }

    @Test
    void dryRunValidatesButNeverCreatesCases() throws Exception {
        RagEvaluationCaseRepository caseRepository = mock(RagEvaluationCaseRepository.class);
        KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
        RagEvaluationService evaluationService = mock(RagEvaluationService.class);
        when(articleRepository.findAll()).thenReturn(List.of(article(7L, true)));
        when(caseRepository.findByDeletedFalse()).thenReturn(List.of());
        RagEvaluationWorkbookService service = service(caseRepository, articleRepository, evaluationService);

        RagEvaluationImportResult result = service.importWorkbook(workbook(
                row("劳动时效", "劳动仲裁时效多久", "7", "", "是")), true, 5L);

        assertTrue(result.isCanImport());
        assertEquals(1, result.getValidCount());
        assertEquals(0, result.getImportedCount());
        verify(evaluationService, never()).createCase(any(), any());
    }

    @Test
    void invalidRowBlocksEntireImport() throws Exception {
        RagEvaluationCaseRepository caseRepository = mock(RagEvaluationCaseRepository.class);
        KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
        RagEvaluationService evaluationService = mock(RagEvaluationService.class);
        when(articleRepository.findAll()).thenReturn(List.of(article(7L, true)));
        when(caseRepository.findByDeletedFalse()).thenReturn(List.of());
        RagEvaluationWorkbookService service = service(caseRepository, articleRepository, evaluationService);

        RagEvaluationImportResult result = service.importWorkbook(workbook(
                row("有效", "问题一", "7", "", "是"),
                row("无效", "问题二", "999", "", "是")), false, 5L);

        assertFalse(result.isCanImport());
        assertEquals(0, result.getImportedCount());
        assertEquals("ERROR", result.getRows().get(1).getStatus());
        verify(evaluationService, never()).createCase(any(), any());
    }

    @Test
    void skipsExistingQuestionAndImportsOnlyNewRows() throws Exception {
        RagEvaluationCaseRepository caseRepository = mock(RagEvaluationCaseRepository.class);
        KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
        RagEvaluationService evaluationService = mock(RagEvaluationService.class);
        when(articleRepository.findAll()).thenReturn(List.of(article(7L, true)));
        RagEvaluationCase existing = new RagEvaluationCase();
        existing.setQuestion("劳动仲裁时效多久");
        when(caseRepository.findByDeletedFalse()).thenReturn(List.of(existing));
        RagEvaluationWorkbookService service = service(caseRepository, articleRepository, evaluationService);

        RagEvaluationImportResult result = service.importWorkbook(workbook(
                row("重复", "劳动仲裁时效多久", "7", "", "是"),
                row("新增", "申请仲裁应提交什么", "7", "", "否")), false, 8L);

        assertTrue(result.isCanImport());
        assertEquals(1, result.getSkippedCount());
        assertEquals(1, result.getImportedCount());
        verify(evaluationService).createCase(argThat(request ->
                request.getQuestion().equals("申请仲裁应提交什么") && !request.getEnabled()), eq(8L));
    }

    @Test
    void rejectsNonXlsxFiles() {
        RagEvaluationWorkbookService service = service(mock(RagEvaluationCaseRepository.class),
                mock(KnowledgeArticleRepository.class), mock(RagEvaluationService.class));
        MockMultipartFile file = new MockMultipartFile("file", "样本.xls",
                "application/vnd.ms-excel", new byte[]{1, 2, 3});

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.importWorkbook(file, true, 1L));

        assertTrue(error.getMessage().contains(".xlsx"));
    }

    private RagEvaluationWorkbookService service(RagEvaluationCaseRepository caseRepository,
                                                   KnowledgeArticleRepository articleRepository,
                                                   RagEvaluationService evaluationService) {
        return new RagEvaluationWorkbookService(caseRepository, articleRepository, evaluationService);
    }

    private KnowledgeArticle article(Long id, boolean indexable) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(id);
        article.setDeleted(false);
        article.setTitle("文档" + id);
        article.setKnowledgeSource(indexable ? "LAW_REGULATION" : "CASE_DEPOSIT");
        article.setKnowledgeEligible(indexable);
        article.setIsPublic(indexable);
        article.setReviewStatus(indexable ? "APPROVED" : "PENDING_REVIEW");
        article.setValidityStatus("EFFECTIVE");
        return article;
    }

    private MockMultipartFile workbook(String[]... rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("评价样本");
            var header = sheet.createRow(0);
            String[] headers = {"样本名称", "评价问题", "预期文档ID", "禁止文档ID", "是否启用"};
            for (int index = 0; index < headers.length; index++) header.createCell(index).setCellValue(headers[index]);
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                var row = sheet.createRow(rowIndex + 1);
                for (int cellIndex = 0; cellIndex < rows[rowIndex].length; cellIndex++) {
                    row.createCell(cellIndex).setCellValue(rows[rowIndex][cellIndex]);
                }
            }
            workbook.write(output);
            return new MockMultipartFile("file", "评价样本.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private String[] row(String name, String question, String expected, String forbidden, String enabled) {
        return new String[]{name, question, expected, forbidden, enabled};
    }
}
