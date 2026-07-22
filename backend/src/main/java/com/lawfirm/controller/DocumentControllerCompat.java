package com.lawfirm.controller;

import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.service.CaseDocumentService;
import com.lawfirm.service.CaseFileLibraryService;
import com.lawfirm.service.CaseService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档管理控制器 - 兼容前端调用
 * 前端调用 /api/documents，但PRD中文档隶属于案件
 * 返回空列表，引导前端使用 /api/cases/{id}/documents
 */
@Slf4j
@RestController
@RequestMapping("documents")
@RequiredArgsConstructor
public class DocumentControllerCompat {

    private final CaseDocumentService caseDocumentService;
    private final CaseFileLibraryService caseFileLibraryService;
    private final CaseService caseService;
    private final SecurityUtils securityUtils;

    /**
     * 获取全部案件文档（聚合视图）
     * GET /api/documents
     * 返回当前用户有权限访问的所有案件的文档
     */
    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public Result<List<CaseDocumentDTO>> getAllDocuments(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) Long caseId) {
        try {
            List<CaseDocumentDTO> documents;

            if (caseId != null) {
                assertCaseVisible(caseId);
                // 查询指定案件的文档
                documents = caseDocumentService.getCaseDocuments(caseId);
            } else if (documentType != null) {
                // 按类型查询所有文档
                documents = caseDocumentService.getDocumentsByType(documentType);
            } else {
                // 查询全部文档（这里需要添加新的Service方法）
                documents = caseDocumentService.getAllDocuments();
            }
            documents = filterVisibleDocuments(documents);

            log.info("获取全部文档列表成功: count={}", documents.size());
            return Result.success(documents);
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return Result.error("获取文档列表失败");
        }
    }

    /**
     * 获取文档详情（兼容接口）
     * GET /api/documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public Result<CaseDocumentDTO> getDocument(@PathVariable Long id) {
        try {
            CaseDocumentDTO result = caseDocumentService.getDocumentById(id);
            assertCaseVisible(result.getCaseId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取文档详情失败", e);
            return Result.error("获取文档详情失败");
        }
    }

    /**
     * 下载文档（兼容接口）
     * GET /api/documents/{id}/download
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public void downloadDocument(@PathVariable Long id, HttpServletResponse response) throws IOException {
        try {
            CaseDocumentDTO document = caseDocumentService.getDocumentById(id);
            assertCaseVisible(document.getCaseId());

            if (document.getFilePath() == null || document.getFilePath().trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件路径不存在");
                return;
            }

            Path filePath = resolveReadableFile(document);
            if (!java.nio.file.Files.isRegularFile(filePath)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件不存在");
                return;
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = java.nio.file.Files.probeContentType(filePath);
            response.setContentType(contentType != null ? contentType : "application/octet-stream");
            String encodedFilename = URLEncoder.encode(document.getDocumentName(), StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encodedFilename);
            try (java.io.InputStream inputStream = resource.getInputStream()) {
                org.springframework.util.StreamUtils.copy(inputStream, response.getOutputStream());
            }
            response.flushBuffer();
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("下载文档失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("下载失败");
        }
    }

    private void assertCaseVisible(Long caseId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        caseService.assertCaseVisible(caseId, currentUserId);
    }

    private List<CaseDocumentDTO> filterVisibleDocuments(List<CaseDocumentDTO> documents) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return documents.stream()
                .filter(document -> document.getCaseId() != null)
                .filter(document -> caseService.canAccessCase(document.getCaseId(), currentUserId))
                .collect(Collectors.toList());
    }

    private Path resolveReadableFile(CaseDocumentDTO document) {
        Path root = caseFileLibraryService.getCaseLibraryRootPath().toAbsolutePath().normalize();
        Path file = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("文档路径超出案件文件库");
        }
        return file;
    }
}
