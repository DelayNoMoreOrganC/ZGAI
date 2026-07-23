package com.lawfirm.service;

import com.lawfirm.entity.ApprovalAttachment;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.ApprovalAttachmentRepository;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SealAttachmentServiceTest {
    @TempDir
    Path tempDir;

    private ApprovalAttachmentRepository attachmentRepository;
    private CaseDocumentRepository caseDocumentRepository;
    private CaseFileLibraryService caseFileLibraryService;
    private SealAttachmentService service;

    @BeforeEach
    void setUp() {
        attachmentRepository = mock(ApprovalAttachmentRepository.class);
        caseDocumentRepository = mock(CaseDocumentRepository.class);
        caseFileLibraryService = mock(CaseFileLibraryService.class);
        service = new SealAttachmentService(
                attachmentRepository,
                caseDocumentRepository,
                mock(UserRepository.class),
                caseFileLibraryService);
        ReflectionTestUtils.setField(service, "approvalFileRoot", tempDir.resolve("approval-files").toString());
        when(attachmentRepository.save(any(ApprovalAttachment.class))).thenAnswer(invocation -> {
            ApprovalAttachment attachment = invocation.getArgument(0);
            attachment.setId(10L);
            return attachment;
        });
    }

    @Test
    void uploadIsHashedAndStoredInsideControlledDirectory() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "法律意见书.pdf", "application/pdf", "content".getBytes());

        var result = service.attachUpload(5L, file, 7L);

        assertEquals("UPLOAD", result.getSourceType());
        assertEquals(64, result.getContentSha256().length());
        assertTrue(Files.isDirectory(tempDir.resolve("approval-files").resolve("5")));
        verify(attachmentRepository).save(any(ApprovalAttachment.class));
    }

    @Test
    void caseDocumentCannotBeReferencedByAnotherCase() {
        CaseDocument document = new CaseDocument();
        document.setId(40L);
        document.setCaseId(31L);
        document.setDeleted(false);
        when(caseDocumentRepository.findById(40L)).thenReturn(Optional.of(document));

        InvalidParameterException error = assertThrows(
                InvalidParameterException.class,
                () -> service.attachCaseDocument(5L, 30L, 40L, 7L));

        assertEquals("参数caseDocumentId无效: 所选文件不属于关联案件", error.getMessage());
    }
}
