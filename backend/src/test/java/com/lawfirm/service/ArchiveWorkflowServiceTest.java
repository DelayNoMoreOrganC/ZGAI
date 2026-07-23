package com.lawfirm.service;

import com.lawfirm.dto.ArchiveJobCreateRequest;
import com.lawfirm.dto.ArchiveReadinessDTO;
import com.lawfirm.dto.ArchiveReviewRequest;
import com.lawfirm.entity.ArchiveDocumentItem;
import com.lawfirm.entity.ArchiveJob;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.*;
import com.lawfirm.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArchiveWorkflowServiceTest {
    private ArchiveJobRepository jobRepository;
    private ArchiveDocumentItemRepository documentItemRepository;
    private ArchiveFieldSnapshotRepository fieldRepository;
    private ArchiveOutputRepository outputRepository;
    private ArchiveAuditLogRepository auditRepository;
    private CaseRepository caseRepository;
    private CaseDocumentRepository caseDocumentRepository;
    private CaseService caseService;
    private CaseFileLibraryService fileLibraryService;
    private CaseTimelineService timelineService;
    private ArchiveWorkerClient workerClient;
    private SecurityUtils securityUtils;
    private ArchiveTransactionRunner transactionRunner;
    private ArchiveWorkflowService service;

    @BeforeEach
    void setUp() {
        jobRepository = mock(ArchiveJobRepository.class);
        documentItemRepository = mock(ArchiveDocumentItemRepository.class);
        fieldRepository = mock(ArchiveFieldSnapshotRepository.class);
        outputRepository = mock(ArchiveOutputRepository.class);
        auditRepository = mock(ArchiveAuditLogRepository.class);
        caseRepository = mock(CaseRepository.class);
        caseDocumentRepository = mock(CaseDocumentRepository.class);
        caseService = mock(CaseService.class);
        fileLibraryService = mock(CaseFileLibraryService.class);
        timelineService = mock(CaseTimelineService.class);
        workerClient = mock(ArchiveWorkerClient.class);
        securityUtils = mock(SecurityUtils.class);
        transactionRunner = mock(ArchiveTransactionRunner.class);
        when(transactionRunner.execute(any())).thenAnswer(invocation -> {
            Supplier<?> callback = invocation.getArgument(0);
            return callback.get();
        });
        service = new ArchiveWorkflowService(
                jobRepository, documentItemRepository, fieldRepository, outputRepository, auditRepository,
                caseRepository, caseDocumentRepository, mock(PartyRepository.class), mock(UserRepository.class),
                caseService, fileLibraryService, timelineService, workerClient,
                securityUtils, transactionRunner);
    }

    @Test
    void readinessRequiresClosedCivilCaseAndReportsCriticalMaterials() {
        Case caseEntity = civilCase("CLOSED");
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));
        when(caseDocumentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        ArchiveReadinessDTO readiness = service.getReadiness(1L, 9L);

        assertTrue(readiness.isCanStart());
        assertFalse(readiness.isReady());
        assertTrue(readiness.getMissingCritical().contains("委托代理合同"));
        assertTrue(readiness.getMissingCritical().contains("授权委托书"));
    }

    @Test
    void createRejectsCaseBeforeClosureWithoutCallingWorker() {
        Case caseEntity = civilCase("ACTIVE");
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));

        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> service.createJob(1L, new ArchiveJobCreateRequest(), 9L));

        assertTrue(error.getMessage().contains("已结案"));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void archivedCaseRequiresCorrectionReasonForNewVersion() {
        Case caseEntity = civilCase("ARCHIVED");
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));

        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> service.createJob(1L, new ArchiveJobCreateRequest(), 9L));

        assertTrue(error.getMessage().contains("更正原因"));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void correctionReadinessExcludesPreviousArchiveArtifacts() {
        Case caseEntity = civilCase("ARCHIVED");
        CaseDocument evidence = new CaseDocument();
        evidence.setId(10L);
        evidence.setDocumentType("证据材料");
        evidence.setOriginalFileName("证据.pdf");
        CaseDocument previousVolume = new CaseDocument();
        previousVolume.setId(11L);
        previousVolume.setDocumentType("电子卷宗");
        previousVolume.setOriginalFileName("旧电子卷宗.pdf");
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));
        when(caseDocumentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(evidence, previousVolume));

        ArchiveReadinessDTO readiness = service.getReadiness(1L, 9L);

        assertEquals(1, readiness.getDocumentCount());
        assertTrue(readiness.getWarnings().stream().anyMatch(value -> value.contains("更正原因")));
    }

    @Test
    void administrativeApprovalRequiresExceptionReasonWhenCriticalMaterialsAreMissing() {
        Case caseEntity = civilCase("CLOSED");
        ArchiveJob job = job("ADMIN_REVIEW");
        when(securityUtils.hasAuthority("CASE_ARCHIVE_REVIEW")).thenReturn(true);
        when(jobRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(job));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));
        when(fieldRepository.findByJobIdAndDeletedFalseOrderByIdAsc(3L)).thenReturn(Collections.emptyList());
        when(documentItemRepository.findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(3L))
                .thenReturn(Collections.emptyList());
        ArchiveReviewRequest request = new ArchiveReviewRequest();
        request.setDecision("APPROVE");

        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> service.review(3L, request, 22L));

        assertTrue(error.getMessage().contains("例外理由"));
        verify(outputRepository, never()).save(any());
    }

    @Test
    void assemblyFailureMarksJobFailedWithoutArchivingCase(@TempDir Path tempDir) throws Exception {
        Case caseEntity = civilCase("CLOSED");
        ArchiveJob job = job("ADMIN_REVIEW");
        List<ArchiveDocumentItem> items = List.of(
                archiveItem(31L, 101L, 3, "委托代理合同.pdf"),
                archiveItem(32L, 102L, 4, "授权委托书.pdf"),
                archiveItem(33L, 103L, 14, "民事判决书.pdf"));
        List<CaseDocument> documents = new java.util.ArrayList<>();
        for (ArchiveDocumentItem item : items) {
            Path source = tempDir.resolve(item.getOriginalFileName());
            Files.writeString(source, "archive fixture");
            CaseDocument document = new CaseDocument();
            document.setId(item.getCaseDocumentId());
            document.setCaseId(1L);
            document.setOriginalFileName(item.getOriginalFileName());
            document.setFilePath(source.toString());
            documents.add(document);
        }
        when(securityUtils.hasAuthority("CASE_ARCHIVE_REVIEW")).thenReturn(true);
        when(jobRepository.findActiveByIdForUpdate(3L)).thenReturn(Optional.of(job));
        when(jobRepository.findById(3L)).thenReturn(Optional.of(job));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));
        when(documentItemRepository.findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(3L))
                .thenReturn(items);
        when(fieldRepository.findByJobIdAndDeletedFalseOrderByIdAsc(3L)).thenReturn(Collections.emptyList());
        when(caseDocumentRepository.findAllById(any())).thenReturn(documents);
        when(fileLibraryService.ensureCaseFolder(caseEntity)).thenReturn(tempDir);
        when(outputRepository.findByCaseIdAndDeletedFalseOrderByVersionNoDesc(1L)).thenReturn(Collections.emptyList());
        when(workerClient.assemble(any())).thenThrow(new IllegalStateException("archive worker unavailable"));
        ArchiveReviewRequest request = new ArchiveReviewRequest();
        request.setDecision("APPROVE");

        var result = service.review(3L, request, 22L);

        assertEquals("FAILED", result.getStatus());
        assertEquals("CLOSED", caseEntity.getStatus());
        assertTrue(result.getErrorMessage().contains("archive worker unavailable"));
        verify(outputRepository, never()).save(any());
        verify(caseRepository, never()).save(any());
        verify(timelineService, never()).createSystemTimeline(anyLong(), anyString(), anyString());
    }

    @Test
    void duplicateSupplementIsRemovedAndNotRegistered(@TempDir Path tempDir) throws Exception {
        Case caseEntity = civilCase("CLOSED");
        ArchiveJob job = job("LAWYER_REVIEW");
        when(jobRepository.findById(3L)).thenReturn(Optional.of(job));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));
        when(fileLibraryService.ensureCaseFolder(caseEntity)).thenReturn(tempDir);
        when(caseDocumentRepository.existsByCaseIdAndContentSha256AndDeletedFalse(eq(1L), anyString()))
                .thenReturn(true);

        assertThrows(InvalidParameterException.class, () -> service.uploadSupplement(3L,
                new MockMultipartFile("file", "合同.pdf", "application/pdf", new byte[]{1, 2, 3}),
                3, 9L));

        verify(caseDocumentRepository, never()).save(any());
        try (java.util.stream.Stream<Path> paths = Files.walk(tempDir)) {
            assertEquals(0, paths.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void supplementFileIsRemovedWhenMetadataRegistrationFails(@TempDir Path tempDir) throws Exception {
        Case caseEntity = civilCase("CLOSED");
        ArchiveJob job = job("LAWYER_REVIEW");
        when(jobRepository.findById(3L)).thenReturn(Optional.of(job));
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));
        when(fileLibraryService.ensureCaseFolder(caseEntity)).thenReturn(tempDir);
        when(caseDocumentRepository.existsByCaseIdAndContentSha256AndDeletedFalse(eq(1L), anyString()))
                .thenReturn(false);
        when(caseDocumentRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> service.uploadSupplement(3L,
                new MockMultipartFile("file", "合同.pdf", "application/pdf", new byte[]{4, 5, 6}),
                3, 9L));

        try (java.util.stream.Stream<Path> paths = Files.walk(tempDir)) {
            assertEquals(0, paths.filter(Files::isRegularFile).count());
        }
    }

    private Case civilCase(String status) {
        Case value = new Case();
        value.setId(1L);
        value.setCaseName("测试案件");
        value.setCaseType("CIVIL");
        value.setStatus(status);
        value.setFilingDate(LocalDate.of(2026, 1, 2));
        value.setCloseDate(LocalDate.of(2026, 7, 24));
        value.setDeleted(false);
        return value;
    }

    private ArchiveJob job(String status) {
        ArchiveJob value = new ArchiveJob();
        value.setId(3L);
        value.setCaseId(1L);
        value.setCreatedBy(9L);
        value.setStatus(status);
        value.setDeleted(false);
        return value;
    }

    private ArchiveDocumentItem archiveItem(Long id, Long documentId, int catalogSeq, String fileName) {
        ArchiveDocumentItem item = new ArchiveDocumentItem();
        item.setId(id);
        item.setJobId(3L);
        item.setCaseDocumentId(documentId);
        item.setOriginalFileName(fileName);
        item.setCatalogSeq(catalogSeq);
        item.setCatalogName("测试目录");
        item.setIncluded(true);
        item.setSortOrder(catalogSeq);
        item.setDeleted(false);
        return item;
    }
}
