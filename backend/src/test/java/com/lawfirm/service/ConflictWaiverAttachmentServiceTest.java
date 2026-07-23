package com.lawfirm.service;

import com.lawfirm.dto.ConflictWaiverAttachmentDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.entity.ConflictCheckRecord;
import com.lawfirm.entity.ConflictWaiverAttachment;
import com.lawfirm.entity.DocumentFolder;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.ConflictCheckRecordRepository;
import com.lawfirm.repository.ConflictWaiverAttachmentRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConflictWaiverAttachmentServiceTest {

    private ConflictWaiverAttachmentRepository attachmentRepository;
    private ConflictCheckRecordRepository recordRepository;
    private CaseDocumentRepository caseDocumentRepository;
    private CaseFileLibraryService caseFileLibraryService;
    private ConflictWaiverAttachmentService service;
    private AtomicReference<ConflictWaiverAttachment> savedAttachment;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        attachmentRepository = mock(ConflictWaiverAttachmentRepository.class);
        recordRepository = mock(ConflictCheckRecordRepository.class);
        caseDocumentRepository = mock(CaseDocumentRepository.class);
        caseFileLibraryService = mock(CaseFileLibraryService.class);
        savedAttachment = new AtomicReference<>();
        service = new ConflictWaiverAttachmentService(
                attachmentRepository,
                recordRepository,
                caseDocumentRepository,
                caseFileLibraryService,
                mock(UserRepository.class));

        when(caseFileLibraryService.getCaseLibraryRootPath()).thenReturn(tempDir);
        when(attachmentRepository.existsByConflictCheckRecordIdAndContentSha256(any(), any())).thenReturn(false);
        when(attachmentRepository.save(any(ConflictWaiverAttachment.class))).thenAnswer(invocation -> {
            ConflictWaiverAttachment attachment = invocation.getArgument(0);
            if (attachment.getId() == null) {
                attachment.setId(41L);
                attachment.setCreatedAt(LocalDateTime.of(2026, 7, 23, 18, 0));
            }
            savedAttachment.set(attachment);
            return attachment;
        });
    }

    @Test
    void rejectsUnsupportedWaiverFileType() {
        when(recordRepository.findById(12L)).thenReturn(Optional.of(pendingRecord()));
        MockMultipartFile file = new MockMultipartFile(
                "file", "说明.txt", "text/plain", "not allowed".getBytes());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.upload(12L, file, 8L));

        assertTrue(error.getMessage().contains("仅支持"));
    }

    @Test
    void uploadsToStagingAndArchivesAsPrivateCaseDocument() throws Exception {
        ConflictCheckRecord record = pendingRecord();
        when(recordRepository.findById(12L)).thenReturn(Optional.of(record));
        MockMultipartFile file = new MockMultipartFile(
                "file", "客户书面豁免.pdf", "application/pdf", "%PDF-1.7 waiver".getBytes());

        ConflictWaiverAttachmentDTO uploaded = service.upload(12L, file, 8L);
        ConflictWaiverAttachment attachment = savedAttachment.get();

        assertEquals("客户书面豁免.pdf", uploaded.getOriginalFileName());
        assertTrue(Files.isRegularFile(Path.of(attachment.getFilePath())));
        assertNotNull(attachment.getContentSha256());

        when(attachmentRepository.findByConflictCheckRecordIdOrderByCreatedAtAsc(12L))
                .thenReturn(Collections.singletonList(attachment));
        Case caseEntity = new Case();
        caseEntity.setId(50L);
        caseEntity.setCaseNumber("LS-2026-CIVIL-50");
        Path caseRoot = tempDir.resolve("2026/LS-2026-CIVIL-50");
        when(caseFileLibraryService.ensureCaseFolder(caseEntity)).thenReturn(caseRoot);
        DocumentFolder folder = new DocumentFolder();
        folder.setId(3L);
        folder.setFolderPath("01_立案材料");
        when(caseFileLibraryService.findCaseFolder(50L, "01_立案材料")).thenReturn(Optional.of(folder));
        when(caseDocumentRepository.save(any(CaseDocument.class))).thenAnswer(invocation -> {
            CaseDocument document = invocation.getArgument(0);
            document.setId(91L);
            return document;
        });

        List<CaseDocument> archived = service.archiveForCase(caseEntity, record, 9L);

        assertEquals(1, archived.size());
        assertEquals("CONFLICT_WAIVER", archived.get(0).getDocumentType());
        assertEquals("FORBIDDEN", archived.get(0).getIndexStatus());
        assertFalse(Boolean.TRUE.equals(archived.get(0).getKnowledgeEligible()));
        assertEquals(91L, attachment.getArchivedDocumentId());
        assertNotNull(attachment.getArchivedAt());
        assertTrue(Files.isRegularFile(Path.of(attachment.getFilePath())));
        assertTrue(Path.of(attachment.getFilePath()).startsWith(caseRoot.resolve("01_立案材料")));
    }

    private ConflictCheckRecord pendingRecord() {
        ConflictCheckRecord record = new ConflictCheckRecord();
        record.setId(12L);
        record.setCaseId(50L);
        record.setSubjectName("测试委托方");
        record.setReviewStatus("PENDING_REVIEW");
        return record;
    }
}
