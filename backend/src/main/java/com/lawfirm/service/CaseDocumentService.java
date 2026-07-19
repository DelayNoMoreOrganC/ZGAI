package com.lawfirm.service;

import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.entity.DocumentFolder;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
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

    /**
     * 上传案件文档
     */
    @Transactional
    public CaseDocumentDTO uploadDocument(Long caseId, MultipartFile file,
                                          String documentType, String folderPath,
                                          Long userId) throws IOException {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("案件不存在"));

        if ("PENDING_APPROVAL".equals(caseEntity.getStatus())) {
            throw new IllegalArgumentException("案件尚未审批通过，不能上传案件文件");
        }

        // 创建目录
        Path caseFolder = caseFileLibraryService.ensureCaseFolder(caseEntity);
        Path uploadPath = caseFolder;
        Long folderId = null;
        if (folderPath != null && !folderPath.trim().isEmpty()) {
            String sanitizedFolderPath = caseFileLibraryService.sanitizeFolderPath(folderPath);
            uploadPath = caseFolder.resolve(sanitizedFolderPath);
            folderPath = sanitizedFolderPath;
            folderId = caseFileLibraryService.findCaseFolder(caseId, folderPath)
                    .map(DocumentFolder::getId)
                    .orElse(null);
        }
        Files.createDirectories(uploadPath);

        // 保存文件
        String originalFilename = file.getOriginalFilename();
        String safeOriginalFilename = sanitizeFileName(originalFilename);
        int versionNo = resolveNextVersion(caseId, folderPath, safeOriginalFilename);
        String newFilename = System.currentTimeMillis() + "_v" + versionNo + "_" + safeOriginalFilename;
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

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
        document.setIndexStatus("NOT_INDEXED");

        CaseDocument saved = caseDocumentRepository.save(document);
        log.info("上传案件文档成功: caseId={}, fileName={}", caseId, originalFilename);

        return convertToDTO(saved);
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "未命名文件";
        }
        return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private int resolveNextVersion(Long caseId, String folderPath, String documentName) {
        return caseDocumentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
                .filter(doc -> documentName.equals(doc.getDocumentName()))
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

        document.setDocumentName(dto.getDocumentName());
        document.setDocumentType(dto.getDocumentType());
        document.setFolderPath(dto.getFolderPath());
        document.setTags(dto.getTags());
        if (dto.getKnowledgeEligible() != null) document.setKnowledgeEligible(dto.getKnowledgeEligible());
        if (dto.getIndexStatus() != null) document.setIndexStatus(dto.getIndexStatus());
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

        // 删除不直接清除NAS文件，先移入文件库回收区，便于误删恢复。
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
            log.warn("移动文件到回收区失败: {}", document.getFilePath(), e);
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

        document.setFolderPath(newFolderPath);
        CaseDocument updated = caseDocumentRepository.save(document);

        log.info("移动案件文档成功: id={}, newFolder={}", id, newFolderPath);
        return convertToDTO(updated);
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
        dto.setTags(document.getTags());
        dto.setKnowledgeEligible(document.getKnowledgeEligible());
        dto.setIndexStatus(document.getIndexStatus());
        dto.setOcrResult(document.getOcrResult());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        return dto;
    }
}
