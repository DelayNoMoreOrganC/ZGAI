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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConflictWaiverAttachmentService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "jpg", "jpeg", "png"));

    private final ConflictWaiverAttachmentRepository attachmentRepository;
    private final ConflictCheckRecordRepository conflictCheckRecordRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseFileLibraryService caseFileLibraryService;
    private final UserRepository userRepository;

    public List<ConflictWaiverAttachmentDTO> list(Long recordId) {
        return attachmentRepository.findByConflictCheckRecordIdOrderByCreatedAtAsc(recordId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public boolean hasAttachment(Long recordId) {
        return !attachmentRepository.findByConflictCheckRecordIdOrderByCreatedAtAsc(recordId).isEmpty();
    }

    @Transactional
    public ConflictWaiverAttachmentDTO upload(Long recordId, MultipartFile file, Long userId) throws IOException {
        ConflictCheckRecord record = requirePendingCaseRecord(recordId);
        validateFile(file);

        Path stagingRoot = caseFileLibraryService.getCaseLibraryRootPath().toAbsolutePath().normalize()
                .resolve(".staging/conflict-waivers")
                .resolve(String.valueOf(recordId))
                .normalize();
        assertInsideLibrary(stagingRoot);
        Files.createDirectories(stagingRoot);

        String safeName = sanitizeFileName(file.getOriginalFilename());
        Path stagedFile = stagingRoot.resolve(UUID.randomUUID() + "_" + safeName).normalize();
        assertInsideLibrary(stagedFile);
        Files.copy(file.getInputStream(), stagedFile);

        String sha256 = sha256(stagedFile);
        if (attachmentRepository.existsByConflictCheckRecordIdAndContentSha256(recordId, sha256)) {
            Files.deleteIfExists(stagedFile);
            throw new IllegalArgumentException("该利冲审查已上传内容相同的豁免依据");
        }

        ConflictWaiverAttachment attachment = new ConflictWaiverAttachment();
        attachment.setConflictCheckRecordId(recordId);
        attachment.setCaseId(record.getCaseId());
        attachment.setOriginalFileName(safeName);
        attachment.setFilePath(stagedFile.toString());
        attachment.setFileSize(Files.size(stagedFile));
        attachment.setMimeType(resolveMimeType(file, stagedFile));
        attachment.setContentSha256(sha256);
        attachment.setUploadedBy(userId);
        return toDTO(attachmentRepository.save(attachment));
    }

    public AttachmentDownload getDownload(Long recordId, Long attachmentId) {
        ConflictWaiverAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("豁免依据附件不存在"));
        if (!recordId.equals(attachment.getConflictCheckRecordId())) {
            throw new IllegalArgumentException("附件不属于当前利冲审查记录");
        }
        Path path = Path.of(attachment.getFilePath()).toAbsolutePath().normalize();
        assertInsideLibrary(path);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("豁免依据原件不存在");
        }
        return new AttachmentDownload(path, attachment.getOriginalFileName(), attachment.getMimeType());
    }

    @Transactional
    public List<CaseDocument> archiveForCase(Case caseEntity, ConflictCheckRecord record, Long archivedBy) {
        List<ConflictWaiverAttachment> attachments = attachmentRepository
                .findByConflictCheckRecordIdOrderByCreatedAtAsc(record.getId());
        if (attachments.isEmpty()) {
            return new ArrayList<>();
        }
        if (!caseEntity.getId().equals(record.getCaseId())) {
            throw new IllegalArgumentException("利冲记录不属于当前案件");
        }

        Path caseRoot = caseFileLibraryService.ensureCaseFolder(caseEntity).toAbsolutePath().normalize();
        DocumentFolder folder = caseFileLibraryService.findCaseFolder(caseEntity.getId(), "01_立案材料")
                .orElseThrow(() -> new IllegalArgumentException("案件立案材料目录不存在"));
        Path targetFolder = caseRoot.resolve(folder.getFolderPath()).normalize();
        assertInsideCaseRoot(caseRoot, targetFolder);
        try {
            Files.createDirectories(targetFolder);
        } catch (IOException e) {
            throw new IllegalArgumentException("创建利冲附件归档目录失败", e);
        }

        List<CaseDocument> archived = new ArrayList<>();
        for (ConflictWaiverAttachment attachment : attachments) {
            if (attachment.getArchivedDocumentId() != null) {
                caseDocumentRepository.findById(attachment.getArchivedDocumentId()).ifPresent(archived::add);
                continue;
            }
            Path source = Path.of(attachment.getFilePath()).toAbsolutePath().normalize();
            assertInsideLibrary(source);
            if (!Files.isRegularFile(source)) {
                throw new IllegalArgumentException("豁免依据原件不存在：" + attachment.getOriginalFileName());
            }

            String archiveName = "利冲豁免依据_" + record.getId() + "_" + attachment.getId() + "_"
                    + sanitizeFileName(attachment.getOriginalFileName());
            Path target = targetFolder.resolve(archiveName).normalize();
            assertInsideCaseRoot(caseRoot, target);
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalArgumentException("归档利冲豁免依据失败：" + attachment.getOriginalFileName(), e);
            }

            CaseDocument document = new CaseDocument();
            document.setCaseId(caseEntity.getId());
            document.setFolderId(folder.getId());
            document.setDocumentName(attachment.getOriginalFileName());
            document.setOriginalFileName(attachment.getOriginalFileName());
            document.setDocumentType("CONFLICT_WAIVER");
            document.setFilePath(target.toString());
            document.setFileSize(attachment.getFileSize());
            document.setMimeType(attachment.getMimeType());
            document.setFolderPath(folder.getFolderPath());
            document.setVersionNo(1);
            document.setUploadBy(archivedBy);
            document.setTags("利冲豁免,利冲记录:" + record.getId());
            document.setKnowledgeEligible(false);
            document.setIndexStatus("FORBIDDEN");
            document.setContentSha256(attachment.getContentSha256());
            try {
                CaseDocument saved = caseDocumentRepository.save(document);
                attachment.setFilePath(target.toString());
                attachment.setArchivedDocumentId(saved.getId());
                attachment.setArchivedAt(LocalDateTime.now());
                attachmentRepository.save(attachment);
                registerFileCompletion(source, target);
                archived.add(saved);
            } catch (RuntimeException e) {
                deleteQuietly(target);
                throw e;
            }
        }
        return archived;
    }

    private ConflictCheckRecord requirePendingCaseRecord(Long recordId) {
        ConflictCheckRecord record = conflictCheckRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("利冲检查记录不存在"));
        if (record.getCaseId() == null) {
            throw new IllegalArgumentException("仅案件立案利冲审查可以上传豁免依据");
        }
        if ("COMPLETED".equals(record.getReviewStatus())) {
            throw new IllegalArgumentException("正式审查已锁定，不允许追加或替换豁免依据");
        }
        return record;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择豁免依据原件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("豁免依据原件不得超过 20MB");
        }
        String name = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(name) && name.lastIndexOf('.') >= 0) {
            extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 PDF、Word、JPG 或 PNG 格式的豁免依据");
        }
    }

    private String sanitizeFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "未命名豁免依据";
        }
        return value.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
    }

    private String resolveMimeType(MultipartFile file, Path path) throws IOException {
        if (StringUtils.hasText(file.getContentType())) {
            return file.getContentType();
        }
        String detected = Files.probeContentType(path);
        return StringUtils.hasText(detected) ? detected : "application/octet-stream";
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path);
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInput.read(buffer) != -1) {
                    // DigestInputStream updates the digest while reading.
                }
            }
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest()) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", e);
        }
    }

    private void assertInsideLibrary(Path path) {
        Path root = caseFileLibraryService.getCaseLibraryRootPath().toAbsolutePath().normalize();
        if (!path.toAbsolutePath().normalize().startsWith(root)) {
            throw new IllegalArgumentException("豁免依据路径超出案件文件库");
        }
    }

    private void assertInsideCaseRoot(Path caseRoot, Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(caseRoot.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("豁免依据归档路径超出案件目录");
        }
    }

    private void registerFileCompletion(Path stagedSource, Path archivedTarget) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(stagedSource);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    deleteQuietly(stagedSource);
                } else {
                    deleteQuietly(archivedTarget);
                }
            }
        });
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A retained staging copy is safer than failing a completed database transaction.
        }
    }

    private ConflictWaiverAttachmentDTO toDTO(ConflictWaiverAttachment attachment) {
        ConflictWaiverAttachmentDTO dto = new ConflictWaiverAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setConflictCheckRecordId(attachment.getConflictCheckRecordId());
        dto.setCaseId(attachment.getCaseId());
        dto.setOriginalFileName(attachment.getOriginalFileName());
        dto.setFileSize(attachment.getFileSize());
        dto.setMimeType(attachment.getMimeType());
        dto.setContentSha256(attachment.getContentSha256());
        dto.setUploadedByName(userRepository.findById(attachment.getUploadedBy())
                .map(user -> StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername())
                .orElse("未知人员"));
        dto.setUploadedAt(attachment.getCreatedAt());
        dto.setArchivedDocumentId(attachment.getArchivedDocumentId());
        dto.setArchivedAt(attachment.getArchivedAt());
        return dto;
    }

    @Getter
    public static class AttachmentDownload {
        private final Path path;
        private final String fileName;
        private final String mimeType;

        public AttachmentDownload(Path path, String fileName, String mimeType) {
            this.path = path;
            this.fileName = fileName;
            this.mimeType = mimeType;
        }
    }
}
