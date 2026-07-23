package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.KnowledgeReviewRequest;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.entity.KnowledgeImportBatch;
import com.lawfirm.entity.KnowledgeImportItem;
import com.lawfirm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnowledgeImportServiceTest {
    @TempDir Path tempDir;
    private KnowledgeImportBatchRepository batches;
    private KnowledgeImportItemRepository items;
    private KnowledgeArticleRepository articles;
    private VectorMigrationService vectorMigrationService;
    private LocalDocumentTextService documentTextService;
    private KnowledgeImportService service;

    @BeforeEach
    void setUp() {
        batches = mock(KnowledgeImportBatchRepository.class);
        items = mock(KnowledgeImportItemRepository.class);
        articles = mock(KnowledgeArticleRepository.class);
        vectorMigrationService = mock(VectorMigrationService.class);
        documentTextService = mock(LocalDocumentTextService.class);
        service = new KnowledgeImportService(batches, items, articles,
                mock(UserRepository.class), vectorMigrationService,
                documentTextService, new ObjectMapper());
        ReflectionTestUtils.setField(service, "policySourceRoot", tempDir.toString());
        ReflectionTestUtils.setField(service, "stagingRoot", tempDir.resolve("stage").toString());
        ReflectionTestUtils.setField(service, "libraryRoot", tempDir.resolve("library").toString());
        ReflectionTestUtils.setField(service, "maxFileSizeBytes", 52_428_800L);
    }

    @Test
    void policyScanIsReadOnlyAndExcludesRecycleHiddenAndTemporaryFiles() throws Exception {
        Path visible = Files.writeString(tempDir.resolve("制度.pdf"), "policy");
        Files.writeString(tempDir.resolve("~$制度.doc"), "temp");
        Files.writeString(tempDir.resolve(".hidden.txt"), "hidden");
        Path recycle = Files.createDirectories(tempDir.resolve("#recycle"));
        Files.writeString(recycle.resolve("old.pdf"), "old");

        AtomicLong itemId = new AtomicLong(1);
        when(batches.save(any())).thenAnswer(invocation -> {
            KnowledgeImportBatch batch = invocation.getArgument(0); batch.setId(7L); return batch;
        });
        when(items.save(any())).thenAnswer(invocation -> {
            KnowledgeImportItem item = invocation.getArgument(0); item.setId(itemId.getAndIncrement()); return item;
        });
        when(items.findFirstByContentSha256AndArticleIdIsNotNullAndDeletedFalse(any())).thenReturn(Optional.empty());
        KnowledgeImportBatch result = service.scanPolicies(9L);

        ArgumentCaptor<KnowledgeImportItem> captor = ArgumentCaptor.forClass(KnowledgeImportItem.class);
        verify(items, times(1)).save(captor.capture());
        assertEquals("制度.pdf", captor.getValue().getSourceRelativePath());
        assertEquals(1, result.getItemCount());
        assertTrue(Files.exists(visible));
        assertEquals("policy", Files.readString(visible));
    }

    @Test
    void starterBatchContainsCuratedCurrentLaws() {
        AtomicLong itemId = new AtomicLong(1);
        when(batches.save(any())).thenAnswer(invocation -> {
            KnowledgeImportBatch batch = invocation.getArgument(0); batch.setId(18L); return batch;
        });
        when(items.save(any())).thenAnswer(invocation -> {
            KnowledgeImportItem item = invocation.getArgument(0); item.setId(itemId.getAndIncrement()); return item;
        });

        KnowledgeImportBatch batch = service.createStarterFlkBatch(9L);

        assertEquals(11, batch.getItemCount());
        ArgumentCaptor<KnowledgeImportItem> captor = ArgumentCaptor.forClass(KnowledgeImportItem.class);
        verify(items, times(11)).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(item -> item.getSourceUrl().contains("58d7569a322b4eca9b22feaa4f5d7d4f")));
    }

    @Test
    void policyConfirmationUsesAutomaticTextExtractionForScannedPdf() throws Exception {
        Path source = Files.write(tempDir.resolve("扫描制度.pdf"), "%PDF scan".getBytes());
        KnowledgeImportBatch batch = batch(7L, "FIRM_POLICY", "DISCOVERED");
        KnowledgeImportItem item = item(11L, 7L, "DISCOVERED");
        item.setTitle("扫描制度");
        item.setOriginalFileName("扫描制度.pdf");
        item.setSourceAbsolutePath(source.toString());
        when(batches.findById(7L)).thenReturn(Optional.of(batch));
        when(items.findByBatchIdAndDeletedFalseOrderByIdAsc(7L)).thenReturn(List.of(item));
        when(articles.findFirstByContentSha256AndDeletedFalse(any())).thenReturn(Optional.empty());
        when(documentTextService.extract(any(), eq("扫描制度.pdf"), any()))
                .thenReturn("第一条 扫描制度由本机自动识别");
        when(articles.save(any())).thenAnswer(invocation -> {
            KnowledgeArticle article = invocation.getArgument(0); article.setId(31L); return article;
        });
        when(items.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(batches.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.confirm(7L, null, 9L);

        verify(documentTextService).extract(any(), eq("扫描制度.pdf"), any());
        assertEquals("PENDING_REVIEW", item.getStatus());
        assertEquals(31L, item.getArticleId());
    }

    @Test
    void pendingFlkPdfCanBeRefreshedFromOfficialDocxBeforeReview() throws Exception {
        Path source = Files.write(tempDir.resolve("民法典.docx"), "official-docx".getBytes());
        KnowledgeImportItem item = item(12L, 7L, "STAGED");
        item.setArticleId(32L);
        item.setOriginalFileName("民法典.docx");
        item.setStagedPath(source.toString());
        item.setContentSha256("new-hash");
        item.setValidityStatus("EFFECTIVE");
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(32L);
        article.setReviewStatus("PENDING_REVIEW");
        article.setContent("旧PDF正文");
        when(articles.findById(32L)).thenReturn(Optional.of(article));
        when(documentTextService.extract(any(), eq("民法典.docx"), any()))
                .thenReturn("中华人民共和国民法典 官方DOCX正文");
        when(articles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReflectionTestUtils.invokeMethod(service, "refreshPendingArticle", item);

        assertEquals("中华人民共和国民法典 官方DOCX正文", article.getContent());
        assertEquals("new-hash", article.getContentSha256());
        assertTrue(article.getAttachmentPath().endsWith(".docx"));
        assertEquals("PENDING_REVIEW", item.getStatus());
        assertNull(item.getErrorMessage());
    }

    @Test
    void flkItemMustBeStagedBeforeConfirmation() {
        KnowledgeImportBatch batch = batch(7L, "FLK", "DISCOVERED");
        KnowledgeImportItem item = item(11L, 7L, "DISCOVERED");
        when(batches.findById(7L)).thenReturn(Optional.of(batch));
        when(items.findByBatchIdAndDeletedFalseOrderByIdAsc(7L)).thenReturn(List.of(item));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.confirm(7L, null, 9L));

        assertTrue(error.getMessage().contains("先完成暂存"));
        assertEquals("DISCOVERED", item.getStatus());
        verify(items, never()).save(item);
    }

    @Test
    void attachmentOnlyAcceptsItemsWaitingForManualUpload() {
        KnowledgeImportBatch batch = batch(7L, "FLK", "DISCOVERED");
        KnowledgeImportItem item = item(11L, 7L, "DISCOVERED");
        when(items.findById(11L)).thenReturn(Optional.of(item));
        when(batches.findById(7L)).thenReturn(Optional.of(batch));
        MockMultipartFile file = new MockMultipartFile("file", "法规.pdf", "application/pdf", "law".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.attach(11L, file));
        verify(items, never()).save(any());
    }

    @Test
    void approvalLocksDecisionUpdatesImportStateAndStartsIndexing() {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(31L);
        article.setReviewStatus("PENDING_REVIEW");
        article.setValidityStatus("EFFECTIVE");
        KnowledgeImportItem item = item(11L, 7L, "PENDING_REVIEW");
        item.setArticleId(31L);
        KnowledgeImportBatch batch = batch(7L, "FIRM_POLICY", "PENDING_REVIEW");
        when(articles.findById(31L)).thenReturn(Optional.of(article));
        when(articles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.findFirstByArticleIdAndDeletedFalse(31L)).thenReturn(Optional.of(item));
        when(items.findByBatchIdAndDeletedFalseOrderByIdAsc(7L)).thenReturn(List.of(item));
        when(batches.findById(7L)).thenReturn(Optional.of(batch));
        when(items.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(batches.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        KnowledgeReviewRequest request = new KnowledgeReviewRequest();
        request.setDecision("APPROVED");

        service.review(31L, request, 5L);

        assertEquals("APPROVED", article.getReviewStatus());
        assertEquals("APPROVED", item.getStatus());
        assertEquals("APPROVED", batch.getStatus());
        verify(vectorMigrationService).indexNewArticle(article);
        assertThrows(IllegalArgumentException.class, () -> service.review(31L, request, 5L));
    }

    private KnowledgeImportBatch batch(Long id, String source, String status) {
        KnowledgeImportBatch batch = new KnowledgeImportBatch();
        batch.setId(id);
        batch.setSourceType(source);
        batch.setStatus(status);
        return batch;
    }

    private KnowledgeImportItem item(Long id, Long batchId, String status) {
        KnowledgeImportItem item = new KnowledgeImportItem();
        item.setId(id);
        item.setBatchId(batchId);
        item.setStatus(status);
        return item;
    }
}
