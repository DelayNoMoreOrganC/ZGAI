package com.lawfirm.service;

import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.entity.DocumentFolder;
import com.lawfirm.enums.CaseStatus;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 案件文档服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseDocumentService {

    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseRepository caseRepository;
    private final CaseFileLibraryService caseFileLibraryService;
    private final UserRepository userRepository;

    /**
     * 上传案件文档
     */
    @Transactional
    public CaseDocumentDTO uploadDocument(Long caseId, MultipartFile file,
                                          String documentType, String folderPath,
                                          Long userId) throws IOException {
        return uploadDocument(caseId, file, documentType, folderPath, userId, null, null);
    }

    @Transactional
    public CaseDocumentDTO uploadDocument(Long caseId, MultipartFile file,
                                          String documentType, String folderPath,
                                          Long userId, String ocrResult, String contentSha256) throws IOException {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("案件不存在"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        assertDocumentEditable(caseEntity);
        if (documentType == null || documentType.trim().isEmpty()) {
            throw new IllegalArgumentException("文件类型不能为空");
        }
        if (contentSha256 != null
                && caseDocumentRepository.existsByCaseIdAndContentSha256AndDeletedFalse(caseId, contentSha256)) {
            throw new IllegalArgumentException("该案件中已存在内容相同的文件");
        }

        // 创建目录
        Path caseFolder = caseFileLibraryService.ensureCaseFolder(caseEntity);
        TargetFolder targetFolder = resolveTargetFolder(caseId, caseFolder, folderPath);
        Path uploadPath = targetFolder.path;
        folderPath = targetFolder.folderPath;
        Long folderId = targetFolder.folderId;
        Files.createDirectories(uploadPath);

        // 保存文件
        String originalFilename = file.getOriginalFilename();
        String safeOriginalFilename = sanitizeFileName(originalFilename);
        int versionNo = resolveNextVersion(caseId, folderPath, safeOriginalFilename);
        String newFilename = System.currentTimeMillis() + "_v" + versionNo + "_" + safeOriginalFilename;
        Path filePath = uploadPath.resolve(newFilename).normalize();
        assertInsideCaseFolder(caseFolder, filePath);
        Files.copy(file.getInputStream(), filePath);
        registerRollbackCleanup(filePath);

        // 创建文档记录
        CaseDocument document = new CaseDocument();
        document.setCaseId(caseId);
        document.setFolderId(folderId);
        document.setDocumentName(safeOriginalFilename);
        document.setOriginalFileName(originalFilename);
        document.setDocumentType(documentType);
        document.setFilePath(filePath.toString());
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setFolderPath(folderPath);
        document.setVersionNo(versionNo);
        document.setUploadBy(userId);
        document.setKnowledgeEligible(false);
        document.setIndexStatus("FORBIDDEN");
        document.setOcrResult(ocrResult);
        document.setContentSha256(contentSha256);

        CaseDocument saved;
        try {
            saved = caseDocumentRepository.save(document);
        } catch (RuntimeException e) {
            deleteQuietly(filePath);
            throw e;
        }
        log.info("上传案件文档成功: caseId={}, fileName={}", caseId, originalFilename);

        return convertToDTO(saved);
    }

    private void registerRollbackCleanup(Path filePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deleteQuietly(filePath);
                }
            }
        });
    }

    private void deleteQuietly(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException cleanupError) {
            log.error("事务回滚后清理案件文件失败: {}", filePath, cleanupError);
        }
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "未命名文件";
        }
        return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private int resolveNextVersion(Long caseId, String folderPath, String documentName) {
        return caseDocumentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
                .filter(doc -> belongsToVersionFamily(doc, documentName))
                .filter(doc -> {
                    if (folderPath == null) {
                        return doc.getFolderPath() == null;
                    }
                    return folderPath.equals(doc.getFolderPath());
                })
                .map(CaseDocument::getVersionNo)
                .filter(version -> version != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    /**
     * 获取案件文档列表
     */
    public List<CaseDocumentDTO> getCaseDocuments(Long caseId) {
        return caseDocumentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取文档列表
     */
    public List<CaseDocumentDTO> getDocumentsByType(String documentType) {
        return caseDocumentRepository.findByDocumentTypeAndDeletedFalse(documentType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据案件和类型获取文档列表
     */
    public List<CaseDocumentDTO> getDocumentsByCaseAndType(Long caseId, String documentType) {
        return caseDocumentRepository.findByCaseIdAndDocumentTypeAndDeletedFalseOrderByCreatedAtDesc(caseId, documentType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取全部文档（跨案件聚合视图）
     */
    public List<CaseDocumentDTO> getAllDocuments() {
        return caseDocumentRepository.findByDeletedFalse().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取文档详情
     */
    public CaseDocumentDTO getDocumentById(Long id) {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new IllegalArgumentException("文档已删除");
        }
        return convertToDTO(document);
    }

    /**
     * 更新文档信息
     */
    @Transactional
    public CaseDocumentDTO updateDocument(Long id, CaseDocumentDTO dto) {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));

        Case ownerCase = caseRepository.findById(document.getCaseId()).orElse(null);
        assertDocumentEditable(ownerCase);

        if (dto.getFolderPath() != null) {
            String requestedFolderPath = normalizeFolderPath(dto.getFolderPath());
            if (!Objects.equals(normalizeFolderPath(document.getFolderPath()), requestedFolderPath)) {
                document = moveDocumentEntity(document, requestedFolderPath);
            }
        }

        if (dto.getDocumentName() != null && !dto.getDocumentName().trim().isEmpty()) {
            document.setDocumentName(sanitizeFileName(dto.getDocumentName()));
        }
        if (dto.getDocumentType() != null && !dto.getDocumentType().trim().isEmpty()) {
            document.setDocumentType(dto.getDocumentType());
        }
        document.setTags(dto.getTags());
        // 第一阶段案件文件不得进入 RAG，避免普通案件编辑权限绕过知识库审核。
        document.setKnowledgeEligible(false);
        document.setIndexStatus("FORBIDDEN");
        document.setOcrResult(dto.getOcrResult());

        CaseDocument updated = caseDocumentRepository.save(document);
        log.info("更新案件文档成功: id={}", id);

        return convertToDTO(updated);
    }

    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long id) {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));

        Case ownerCase = caseRepository.findById(document.getCaseId()).orElse(null);
        assertDocumentEditable(ownerCase);

        // 删除不直接清除 NAS 文件，先移入文件库回收区，便于误删恢复。
        try {
            if (document.getFilePath() != null && !document.getFilePath().trim().isEmpty()) {
                Path filePath = Paths.get(document.getFilePath());
                if (Files.exists(filePath)) {
                    Path trashDir = caseFileLibraryService.getCaseLibraryRootPath()
                            .resolve(".trash")
                            .resolve(LocalDate.now().toString())
                            .resolve(String.valueOf(document.getCaseId()));
                    Files.createDirectories(trashDir);
                    Path trashPath = trashDir.resolve(System.currentTimeMillis() + "_" + filePath.getFileName());
                    Files.move(filePath, trashPath, StandardCopyOption.REPLACE_EXISTING);
                    document.setFilePath(trashPath.toString());
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("文件移入回收区失败，未删除记录: " + e.getMessage(), e);
        }

        document.setDeleted(true);
        caseDocumentRepository.save(document);
        log.info("删除案件文档成功: id={}", id);
    }

    /**
     * 移动文档到其他文件夹
     */
    @Transactional
    public CaseDocumentDTO moveDocument(Long id, String newFolderPath) {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));

        Case ownerCase = caseRepository.findById(document.getCaseId()).orElse(null);
        assertDocumentEditable(ownerCase);

        document = moveDocumentEntity(document, normalizeFolderPath(newFolderPath));
        CaseDocument updated = caseDocumentRepository.save(document);

        log.info("移动案件文档成功: id={}, newFolder={}", id, newFolderPath);
        return convertToDTO(updated);
    }

    private CaseDocument moveDocumentEntity(CaseDocument document, String newFolderPath) {
        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new IllegalArgumentException("文档已删除，不能移动");
        }

        Case caseEntity = caseRepository.findById(document.getCaseId())
                .orElseThrow(() -> new IllegalArgumentException("案件不存在"));
        Path caseFolder = caseFileLibraryService.ensureCaseFolder(caseEntity);
        TargetFolder targetFolder = resolveTargetFolder(document.getCaseId(), caseFolder, newFolderPath);

        String oldFolderPath = normalizeFolderPath(document.getFolderPath());
        if (Objects.equals(oldFolderPath, targetFolder.folderPath)) {
            document.setFolderId(targetFolder.folderId);
            document.setFolderPath(targetFolder.folderPath);
            return document;
        }

        if (document.getFilePath() != null && !document.getFilePath().trim().isEmpty()) {
            try {
                Path sourcePath = Paths.get(document.getFilePath()).normalize();
                assertInsideCaseFolder(caseFolder, sourcePath);
                if (Files.exists(sourcePath)) {
                    Files.createDirectories(targetFolder.path);
                    Path targetPath = targetFolder.path.resolve(sourcePath.getFileName()).normalize();
                    assertInsideCaseFolder(caseFolder, targetPath);
                    if (Files.exists(targetPath)) {
                        targetPath = targetFolder.path.resolve(System.currentTimeMillis() + "_" + sourcePath.getFileName()).normalize();
                        assertInsideCaseFolder(caseFolder, targetPath);
                    }
                    Files.move(sourcePath, targetPath);
                    document.setFilePath(targetPath.toString());
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("移动文档文件失败: " + e.getMessage(), e);
            }
        }

        document.setFolderId(targetFolder.folderId);
        document.setFolderPath(targetFolder.folderPath);
        return document;
    }

    private TargetFolder resolveTargetFolder(Long caseId, Path caseFolder, String folderPath) {
        String normalizedFolderPath = normalizeFolderPath(folderPath);
        if (normalizedFolderPath == null) {
            throw new IllegalArgumentException("请选择标准案件目录");
        }

        DocumentFolder folder = caseFileLibraryService.findCaseFolder(caseId, normalizedFolderPath)
                .orElseThrow(() -> new IllegalArgumentException("案件目录不存在，请从标准目录中选择"));
        Path targetPath = caseFolder.resolve(folder.getFolderPath()).normalize();
        assertInsideCaseFolder(caseFolder, targetPath);
        return new TargetFolder(folder.getId(), folder.getFolderPath(), targetPath);
    }

    private String normalizeFolderPath(String folderPath) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return null;
        }
        return caseFileLibraryService.sanitizeFolderPath(folderPath);
    }

    private void assertInsideCaseFolder(Path caseFolder, Path targetPath) {
        Path normalizedRoot = caseFolder.toAbsolutePath().normalize();
        Path normalizedTarget = targetPath.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("文件路径超出案件目录范围");
        }
    }

   /**
    * 查询同名文档的版本历史：按版本号倒序返回同目录下同名文件的各版本。
    */
    public List<CaseDocumentDTO> getVersionHistory(Long caseId, Long documentId) {
        CaseDocument document = caseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new IllegalArgumentException("文档已删除");
        }
        if (!document.getCaseId().equals(caseId)) {
            throw new IllegalArgumentException("文档不属于该案件");
        }
        final String familyName = getVersionFamilyName(document);
        final String folder = normalizeFolderPath(document.getFolderPath());
        return caseDocumentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
                .filter(doc -> belongsToVersionFamily(doc, familyName))
                .filter(doc -> Objects.equals(folder, normalizeFolderPath(doc.getFolderPath())))
                .sorted((a, b) -> Integer.compare(
                        b.getVersionNo() == null ? 0 : b.getVersionNo(),
                        a.getVersionNo() == null ? 0 : a.getVersionNo()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private boolean belongsToVersionFamily(CaseDocument document, String familyName) {
        return familyName != null && familyName.equals(getVersionFamilyName(document));
    }

    private String getVersionFamilyName(CaseDocument document) {
        String sourceName = document.getOriginalFileName();
        if (sourceName == null || sourceName.trim().isEmpty()) {
            sourceName = document.getDocumentName();
        }
        return sanitizeFileName(sourceName);
    }

    /**
     * 案卷仅在立案审批通过后的审理中状态可修改。
     */
    private void assertDocumentEditable(Case caseEntity) {
        if (caseEntity == null) {
            throw new IllegalArgumentException("案件不存在");
        }
        CaseStatus status = CaseStatus.fromCode(caseEntity.getStatus());
        if (status != CaseStatus.ACTIVE) {
            String statusName = status == null ? "未知状态" : status.getDescription();
            throw new IllegalArgumentException("案件「" + statusName + "」，案卷当前只读");
        }
    }

    /**
    * 转换为DTO
    */
    private CaseDocumentDTO convertToDTO(CaseDocument document) {
        CaseDocumentDTO dto = new CaseDocumentDTO();
        dto.setId(document.getId());
        dto.setCaseId(document.getCaseId());
        dto.setFolderId(document.getFolderId());
        dto.setDocumentName(document.getDocumentName());
        dto.setOriginalFileName(document.getOriginalFileName());
        dto.setDocumentType(document.getDocumentType());
        dto.setFilePath(document.getFilePath());
        dto.setFileSize(document.getFileSize());
        dto.setMimeType(document.getMimeType());
        dto.setFolderPath(document.getFolderPath());
        dto.setVersionNo(document.getVersionNo());
        dto.setUploadBy(document.getUploadBy());
        dto.setUploadByName(resolveUploadByName(document.getUploadBy()));
        dto.setTags(document.getTags());
        dto.setKnowledgeEligible(document.getKnowledgeEligible());
        dto.setIndexStatus(document.getIndexStatus());
        dto.setOcrResult(document.getOcrResult());
        dto.setContentSha256(document.getContentSha256());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        return dto;
    }

    private String resolveUploadByName(Long uploadBy) {
        if (uploadBy == null) {
            return "未知";
        }
        return userRepository.findById(uploadBy)
                .map(user -> user.getRealName() != null && !user.getRealName().trim().isEmpty()
                        ? user.getRealName()
                        : user.getUsername())
                .orElse("未知");
    }

    private static class TargetFolder {
        private final Long folderId;
        private final String folderPath;
        private final Path path;

        private TargetFolder(Long folderId, String folderPath, Path path) {
            this.folderId = folderId;
            this.folderPath = folderPath;
            this.path = path;
        }
    }
}
