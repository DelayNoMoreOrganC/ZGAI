package com.lawfirm.service;

import com.lawfirm.dto.ApprovalAttachmentDTO;
import com.lawfirm.entity.ApprovalAttachment;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.ApprovalAttachmentRepository;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SealAttachmentService {
    private static final long MAX_FILE_SIZE = 52_428_800L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg");

    private final ApprovalAttachmentRepository attachmentRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final UserRepository userRepository;
    private final CaseFileLibraryService caseFileLibraryService;

    @Value("${approval.file-root:./data/approval-files}")
    private String approvalFileRoot;

    @Transactional
    public ApprovalAttachmentDTO attachUpload(Long approvalId, MultipartFile file, Long userId) {
        validateUpload(file);
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "用印文件";
        Path root = Paths.get(approvalFileRoot).toAbsolutePath().normalize();
        Path target = root.resolve(String.valueOf(approvalId))
                .resolve(UUID.randomUUID() + "_" + safeFileName(originalName)).normalize();
        ensureInside(root, target);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            ApprovalAttachment attachment = new ApprovalAttachment();
            attachment.setApprovalId(approvalId);
            attachment.setOriginalFileName(originalName);
            attachment.setFilePath(target.toString());
            attachment.setFileSize(Files.size(target));
            attachment.setMimeType(file.getContentType());
            attachment.setContentSha256(sha256(target));
            attachment.setSourceType("UPLOAD");
            attachment.setUploadedBy(userId);
            return toDTO(attachmentRepository.save(attachment));
        } catch (RuntimeException e) {
            try { Files.deleteIfExists(target); } catch (Exception ignored) { }
            throw e;
        } catch (Exception e) {
            try { Files.deleteIfExists(target); } catch (Exception ignored) { }
            throw new IllegalStateException("用印文件保存失败", e);
        }
    }

    @Transactional
    public ApprovalAttachmentDTO attachCaseDocument(Long approvalId, Long caseId, Long caseDocumentId, Long userId) {
        CaseDocument document = caseDocumentRepository.findById(caseDocumentId)
                .filter(value -> !Boolean.TRUE.equals(value.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("案件文档", caseDocumentId));
        if (!caseId.equals(document.getCaseId())) {
            throw new InvalidParameterException("caseDocumentId", "所选文件不属于关联案件");
        }
        Path root = caseFileLibraryService.getCaseLibraryRootPath().toAbsolutePath().normalize();
        Path source = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
        ensureInside(root, source);
        if (!Files.isRegularFile(source)) throw new ResourceNotFoundException("案件文档文件", caseDocumentId);

        ApprovalAttachment attachment = new ApprovalAttachment();
        attachment.setApprovalId(approvalId);
        attachment.setCaseDocumentId(caseDocumentId);
        attachment.setOriginalFileName(StringUtils.hasText(document.getOriginalFileName())
                ? document.getOriginalFileName() : document.getDocumentName());
        attachment.setFilePath(source.toString());
        attachment.setFileSize(document.getFileSize());
        attachment.setMimeType(document.getMimeType());
        attachment.setContentSha256(StringUtils.hasText(document.getContentSha256())
                ? document.getContentSha256() : sha256(source));
        attachment.setSourceType("CASE_DOCUMENT");
        attachment.setUploadedBy(userId);
        return toDTO(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<ApprovalAttachmentDTO> list(Long approvalId) {
        return attachmentRepository.findByApprovalIdAndDeletedFalseOrderByIdAsc(approvalId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Path getDownloadPath(Long approvalId, Long attachmentId) {
        ApprovalAttachment attachment = attachmentRepository.findByIdAndApprovalIdAndDeletedFalse(attachmentId, approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("用印审批附件", attachmentId));
        Path path = Paths.get(attachment.getFilePath()).toAbsolutePath().normalize();
        Path root = "CASE_DOCUMENT".equals(attachment.getSourceType())
                ? caseFileLibraryService.getCaseLibraryRootPath().toAbsolutePath().normalize()
                : Paths.get(approvalFileRoot).toAbsolutePath().normalize();
        ensureInside(root, path);
        if (!Files.isRegularFile(path)) throw new ResourceNotFoundException("用印审批附件文件", attachmentId);
        return path;
    }

    @Transactional
    public void markDecision(Long approvalId, String status, Long approverId, LocalDateTime decidedAt) {
        for (ApprovalAttachment attachment : attachmentRepository.findByApprovalIdAndDeletedFalseOrderByIdAsc(approvalId)) {
            attachment.setSealStatus(status);
            attachment.setDecidedBy(approverId);
            attachment.setDecidedAt(decidedAt);
            attachmentRepository.save(attachment);
        }
    }

    private ApprovalAttachmentDTO toDTO(ApprovalAttachment value) {
        ApprovalAttachmentDTO dto = new ApprovalAttachmentDTO();
        dto.setId(value.getId()); dto.setApprovalId(value.getApprovalId()); dto.setCaseDocumentId(value.getCaseDocumentId());
        dto.setOriginalFileName(value.getOriginalFileName()); dto.setFileSize(value.getFileSize()); dto.setMimeType(value.getMimeType());
        dto.setContentSha256(value.getContentSha256()); dto.setSourceType(value.getSourceType()); dto.setSealStatus(value.getSealStatus());
        dto.setUploadedBy(value.getUploadedBy()); dto.setUploadedByName(userName(value.getUploadedBy()));
        dto.setDecidedBy(value.getDecidedBy()); dto.setDecidedByName(userName(value.getDecidedBy())); dto.setDecidedAt(value.getDecidedAt());
        return dto;
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new InvalidParameterException("file", "请选择需要用印的文件");
        if (file.getSize() > MAX_FILE_SIZE) throw new InvalidParameterException("file", "用印文件不能超过50MB");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidParameterException("file", "用印文件仅支持PDF、Word、Excel和图片格式");
        }
    }

    private String userName(Long userId) {
        return userId == null ? null : userRepository.findById(userId).map(value -> value.getRealName()).orElse(null);
    }
    private String safeFileName(String value) { return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_"); }
    private void ensureInside(Path root, Path path) {
        if (!path.startsWith(root)) throw new IllegalStateException("用印附件路径越界");
    }
    private String sha256(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192]; int read;
            while ((read = input.read(buffer)) > 0) digest.update(buffer, 0, read);
            StringBuilder out = new StringBuilder();
            for (byte value : digest.digest()) out.append(String.format("%02x", value));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("用印文件哈希计算失败", e);
        }
    }
}
