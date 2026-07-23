package com.lawfirm.service;

import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.entity.DocumentFolder;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CaseDocumentServiceTest {

    private CaseDocumentRepository documentRepository;
    private CaseRepository caseRepository;
    private CaseFileLibraryService fileLibraryService;
    private UserRepository userRepository;
    private CaseDocumentService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(CaseDocumentRepository.class);
        caseRepository = mock(CaseRepository.class);
        fileLibraryService = mock(CaseFileLibraryService.class);
        userRepository = mock(UserRepository.class);
        service = new CaseDocumentService(
                documentRepository,
                caseRepository,
                fileLibraryService,
                userRepository);
    }

    @Test
    void rejectsUploadBeforeFilingApproval() {
        Case caseEntity = caseWithStatus("PENDING_APPROVAL");
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadDocument(
                        1L,
                        new MockMultipartFile("file", "证据.docx", "application/octet-stream", new byte[]{1}),
                        "证据材料",
                        "02_证据材料",
                        9L));

        assertTrue(error.getMessage().contains("只读"));
        verifyNoInteractions(fileLibraryService);
    }

    @Test
    void uploadsToStandardFolderAsNewVersionAndForbidsRag(@TempDir Path tempDir) throws Exception {
        Case caseEntity = caseWithStatus("ACTIVE");
        DocumentFolder folder = standardFolder();
        CaseDocument firstVersion = document(5L, 1, "已重命名.docx", "证据.docx");

        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));
        when(fileLibraryService.ensureCaseFolder(caseEntity)).thenReturn(tempDir);
        when(fileLibraryService.sanitizeFolderPath("02_证据材料")).thenReturn("02_证据材料");
        when(fileLibraryService.findCaseFolder(1L, "02_证据材料")).thenReturn(Optional.of(folder));
        when(documentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(firstVersion));
        when(documentRepository.save(any(CaseDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        CaseDocumentDTO result = service.uploadDocument(
                1L,
                new MockMultipartFile("file", "证据.docx", "application/octet-stream", new byte[]{1, 2, 3}),
                "证据材料",
                "02_证据材料",
                9L);

        assertEquals(2, result.getVersionNo());
        assertEquals(folder.getId(), result.getFolderId());
        assertFalse(result.getKnowledgeEligible());
        assertEquals("FORBIDDEN", result.getIndexStatus());
        assertTrue(Files.exists(Path.of(result.getFilePath())));
    }

    @Test
    void removesCopiedFileWhenMetadataSaveFails(@TempDir Path tempDir) {
        Case caseEntity = caseWithStatus("ACTIVE");
        DocumentFolder folder = standardFolder();
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));
        when(fileLibraryService.ensureCaseFolder(caseEntity)).thenReturn(tempDir);
        when(fileLibraryService.sanitizeFolderPath("02_证据材料")).thenReturn("02_证据材料");
        when(fileLibraryService.findCaseFolder(1L, "02_证据材料")).thenReturn(Optional.of(folder));
        when(documentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.emptyList());
        when(documentRepository.save(any(CaseDocument.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> service.uploadDocument(
                1L,
                new MockMultipartFile("file", "证据.docx", "application/octet-stream", new byte[]{1, 2, 3}),
                "证据材料",
                "02_证据材料",
                9L));

        Path targetFolder = tempDir.resolve("02_证据材料");
        assertTrue(Files.notExists(targetFolder) || isEmptyDirectory(targetFolder));
    }

    @Test
    void removesCopiedFileWhenOuterTransactionRollsBack(@TempDir Path tempDir) throws Exception {
        Case caseEntity = caseWithStatus("ACTIVE");
        DocumentFolder folder = standardFolder();
        when(caseRepository.findById(1L)).thenReturn(Optional.of(caseEntity));
        when(fileLibraryService.ensureCaseFolder(caseEntity)).thenReturn(tempDir);
        when(fileLibraryService.sanitizeFolderPath("02_证据材料")).thenReturn("02_证据材料");
        when(fileLibraryService.findCaseFolder(1L, "02_证据材料")).thenReturn(Optional.of(folder));
        when(documentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.emptyList());
        when(documentRepository.save(any(CaseDocument.class))).thenAnswer(call -> call.getArgument(0));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        TransactionSynchronizationManager.initSynchronization();
        try {
            CaseDocumentDTO result = service.uploadDocument(
                    1L,
                    new MockMultipartFile("file", "证据.docx", "application/octet-stream", new byte[]{1, 2, 3}),
                    "证据材料", "02_证据材料", 9L);
            Path copied = Path.of(result.getFilePath());
            assertTrue(Files.exists(copied));

            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertTrue(Files.notExists(copied));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void returnsVersionFamilyByOriginalNameInDescendingOrder() {
        CaseDocument current = document(12L, 2, "证据_定稿.docx", "证据.docx");
        CaseDocument first = document(11L, 1, "证据_初稿.docx", "证据.docx");
        CaseDocument unrelated = document(13L, 7, "其他.docx", "其他.docx");

        when(documentRepository.findById(12L)).thenReturn(Optional.of(current));
        when(documentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(first, unrelated, current));
        when(fileLibraryService.sanitizeFolderPath("02_证据材料")).thenReturn("02_证据材料");
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        java.util.List<CaseDocumentDTO> versions = service.getVersionHistory(1L, 12L);

        assertEquals(2, versions.size());
        assertEquals(2, versions.get(0).getVersionNo());
        assertEquals(1, versions.get(1).getVersionNo());
    }

    private Case caseWithStatus(String status) {
        Case caseEntity = new Case();
        caseEntity.setId(1L);
        caseEntity.setStatus(status);
        return caseEntity;
    }

    private DocumentFolder standardFolder() {
        DocumentFolder folder = new DocumentFolder();
        folder.setId(20L);
        folder.setCaseId(1L);
        folder.setFolderPath("02_证据材料");
        return folder;
    }

    private CaseDocument document(Long id, int version, String displayName, String originalName) {
        CaseDocument document = new CaseDocument();
        document.setId(id);
        document.setCaseId(1L);
        document.setFolderPath("02_证据材料");
        document.setDocumentName(displayName);
        document.setOriginalFileName(originalName);
        document.setDocumentType("证据材料");
        document.setFilePath("/tmp/" + id + "_" + originalName);
        document.setVersionNo(version);
        document.setUploadBy(9L);
        document.setDeleted(false);
        return document;
    }

    private boolean isEmptyDirectory(Path path) {
        try (java.util.stream.Stream<Path> files = Files.list(path)) {
            return files.findAny().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
