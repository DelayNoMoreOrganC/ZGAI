package com.lawfirm.service;

import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.entity.CaseDocument;
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

    private static final String UPLOAD_BASE_DIR = "uploads/documents/";

    /**
     * 上传案件文档
     */
    @Transactional
    public CaseDocumentDTO uploadDocument(Long caseId, MultipartFile file,
                                          String documentType, String folderPath,
                                          Long userId) throws IOException {
        // 验证案件是否存在
        if (!caseRepository.existsById(caseId)) {
            throw new IllegalArgumentException("案件不存在");
        }

        // 创建目录
        Path uploadPath = Paths.get(UPLOAD_BASE_DIR, caseId.toString());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 保存文件
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

        // 创建文档记录
        CaseDocument document = new CaseDocument();
        document.setCaseId(caseId);
        document.setDocumentName(originalFilename);
        document.setDocumentType(documentType);
        document.setFilePath(filePath.toString());
        document.setFileSize(file.getSize());
        document.setFolderPath(folderPath);
        document.setUploadBy(userId);

        CaseDocument saved = caseDocumentRepository.save(document);
        log.info("上传案件文档成功: caseId={}, fileName={}", caseId, originalFilename);

        return convertToDTO(saved);
    }

    /**
     * 获取案件文档列表
     */
    public List<CaseDocumentDTO> getCaseDocuments(Long caseId) {
        return caseDocumentRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取文档列表
     */
    public List<CaseDocumentDTO> getDocumentsByType(String documentType) {
        return caseDocumentRepository.findByDocumentType(documentType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取全部文档（跨案件聚合视图）
     */
    public List<CaseDocumentDTO> getAllDocuments() {
        return caseDocumentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取文档详情
     */
    public CaseDocumentDTO getDocumentById(Long id) {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
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

        // 删除物理文件
        try {
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.warn("删除物理文件失败: {}", document.getFilePath(), e);
        }

        // 删除数据库记录
        caseDocumentRepository.deleteById(id);
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
        dto.setDocumentName(document.getDocumentName());
        dto.setDocumentType(document.getDocumentType());
        dto.setFilePath(document.getFilePath());
        dto.setFileSize(document.getFileSize());
        dto.setFolderPath(document.getFolderPath());
        dto.setUploadBy(document.getUploadBy());
        dto.setTags(document.getTags());
        dto.setOcrResult(document.getOcrResult());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        return dto;
    }
}
