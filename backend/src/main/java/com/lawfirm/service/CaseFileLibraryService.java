package com.lawfirm.service;

import com.lawfirm.entity.Case;
import com.lawfirm.entity.DocumentFolder;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.DocumentFolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 案件文件库服务：负责目录模板和一案一档目录生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseFileLibraryService {

    private final DocumentFolderRepository documentFolderRepository;
    private final CaseRepository caseRepository;

    @Value("${file.case-library-root:./case-files}")
    private String caseLibraryRoot;

    private static final List<DefaultFolder> DEFAULT_FOLDERS = Arrays.asList(
            new DefaultFolder("01", "01_立案材料", "01_立案材料", 10),
            new DefaultFolder("02", "02_证据材料", "02_证据材料", 20),
            new DefaultFolder("03", "03_法律文书", "03_法律文书", 30),
            new DefaultFolder("04", "04_合同收费", "04_合同收费", 40),
            new DefaultFolder("05", "05_往来函件", "05_往来函件", 50),
            new DefaultFolder("99", "99_归档材料", "99_归档材料", 990)
    );

    @PostConstruct
    @Transactional
    public void initializeDefaultTemplates() {
        for (DefaultFolder folder : DEFAULT_FOLDERS) {
            if (documentFolderRepository.existsByFolderTypeAndFolderCodeAndDeletedFalse("TEMPLATE", folder.code)) {
                continue;
            }
            DocumentFolder template = new DocumentFolder();
            template.setFolderCode(folder.code);
            template.setFolderName(folder.name);
            template.setFolderPath(folder.path);
            template.setFolderType("TEMPLATE");
            template.setSortOrder(folder.sortOrder);
            template.setSystemDefault(true);
            documentFolderRepository.save(template);
        }
    }

    @Transactional
    public Path ensureCaseFolder(Case caseEntity) {
        if (caseEntity.getCaseFolderPath() != null && !caseEntity.getCaseFolderPath().trim().isEmpty()) {
            Path existing = Paths.get(caseEntity.getCaseFolderPath());
            createDirectories(existing);
            ensureCaseSubFolders(caseEntity, existing);
            return existing;
        }

        String year = String.valueOf(Year.now().getValue());
        String caseNo = sanitizePathSegment(caseEntity.getCaseNumber() != null ? caseEntity.getCaseNumber() : "CASE_" + caseEntity.getId());
        Path root = Paths.get(caseLibraryRoot, year, caseNo);
        createDirectories(root);
        caseEntity.setCaseFolderPath(root.toString());
        caseRepository.save(caseEntity);
        ensureCaseSubFolders(caseEntity, root);
        return root;
    }

    @Transactional
    public void ensureCaseSubFolders(Case caseEntity, Path caseRoot) {
        List<DocumentFolder> existingFolders = documentFolderRepository.findByCaseIdAndDeletedFalseOrderBySortOrderAsc(caseEntity.getId());
        if (!existingFolders.isEmpty()) {
            existingFolders.forEach(folder -> createDirectories(caseRoot.resolve(folder.getFolderPath())));
            return;
        }

        List<DocumentFolder> templates = documentFolderRepository
                .findByFolderTypeAndActiveTrueAndDeletedFalseOrderBySortOrderAsc("TEMPLATE");
        for (DocumentFolder template : templates) {
            DocumentFolder caseFolder = new DocumentFolder();
            caseFolder.setCaseId(caseEntity.getId());
            caseFolder.setFolderCode(template.getFolderCode());
            caseFolder.setFolderName(template.getFolderName());
            caseFolder.setFolderPath(template.getFolderPath());
            caseFolder.setFolderType("CASE");
            caseFolder.setSortOrder(template.getSortOrder());
            caseFolder.setSystemDefault(template.getSystemDefault());
            documentFolderRepository.save(caseFolder);
            createDirectories(caseRoot.resolve(caseFolder.getFolderPath()));
        }
    }

    public Optional<DocumentFolder> findCaseFolder(Long caseId, String folderPath) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return Optional.empty();
        }
        return documentFolderRepository.findByCaseIdAndFolderPathAndDeletedFalse(caseId, sanitizeFolderPath(folderPath));
    }

    public List<DocumentFolder> getCaseFolders(Long caseId) {
        return documentFolderRepository.findByCaseIdAndDeletedFalseOrderBySortOrderAsc(caseId);
    }

    @Transactional
    public List<DocumentFolder> getOrCreateCaseFolders(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("案件不存在"));
        ensureCaseFolder(caseEntity);
        return getCaseFolders(caseId);
    }

    public Path getCaseLibraryRootPath() {
        return Paths.get(caseLibraryRoot);
    }

    public String sanitizeFolderPath(String folderPath) {
        return folderPath.replace("\\", "/")
                .replaceAll("\\.\\.", "")
                .replaceAll("^/+", "")
                .replaceAll("[*?\"<>|]+", "_");
    }

    private String sanitizePathSegment(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "未命名案件";
        }
        return value.replaceAll("[\\\\/:*?\"<>|\\s\\[\\]]+", "_");
    }

    private void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("创建案件文件夹失败: " + e.getMessage(), e);
        }
    }

    private static class DefaultFolder {
        private final String code;
        private final String name;
        private final String path;
        private final Integer sortOrder;

        private DefaultFolder(String code, String name, String path, Integer sortOrder) {
            this.code = code;
            this.name = name;
            this.path = path;
            this.sortOrder = sortOrder;
        }
    }
}
