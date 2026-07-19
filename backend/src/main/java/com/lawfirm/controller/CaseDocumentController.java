package com.lawfirm.controller;

import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.entity.DocumentFolder;
import com.lawfirm.service.CaseFileLibraryService;
import com.lawfirm.service.CaseDocumentService;
import com.lawfirm.service.CaseService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 案件文档控制器
 */
@Slf4j
@RestController
@RequestMapping("cases/{caseId}/documents")
@RequiredArgsConstructor
public class CaseDocumentController {

    private final CaseDocumentService caseDocumentService;
    private final CaseFileLibraryService caseFileLibraryService;
    private final CaseService caseService;
    private final SecurityUtils securityUtils;

    /**
     * 上传案件文档
     * POST /api/cases/{caseId}/documents
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<CaseDocumentDTO> uploadDocument(
            @PathVariable Long caseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "folderPath", required = false) String folderPath) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            caseService.assertCaseVisible(caseId, userId);
            CaseDocumentDTO result = caseDocumentService.uploadDocument(
                    caseId, file, documentType, folderPath, userId);
            return Result.success("文档上传成功", result);
        } catch (IllegalArgumentException e) {
            log.error("上传文档失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("上传文档异常", e);
            return Result.error("文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取案件文档列表
     * GET /api/cases/{caseId}/documents
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CaseDocumentDTO>> getCaseDocuments(@PathVariable Long caseId) {
        try {
            assertCaseVisible(caseId);
            List<CaseDocumentDTO> documents = caseDocumentService.getCaseDocuments(caseId);
            return Result.success(documents);
        } catch (Exception e) {
            log.error("获取案件文档列表异常", e);
            return Result.error("获取案件文档列表失败");
        }
    }

    /**
     * 获取案件文件夹目录
     * GET /api/cases/{caseId}/documents/folders
     */
    @GetMapping("/folders")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<DocumentFolder>> getCaseFolders(@PathVariable Long caseId) {
        try {
            assertCaseVisible(caseId);
            return Result.success(caseFileLibraryService.getOrCreateCaseFolders(caseId));
        } catch (Exception e) {
            log.error("获取案件文件夹目录异常", e);
            return Result.error("获取案件文件夹目录失败");
        }
    }

    /**
     * 根据类型获取文档列表
     * GET /api/cases/{caseId}/documents/type/{documentType}
     */
    @GetMapping("/type/{documentType}")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CaseDocumentDTO>> getDocumentsByType(
            @PathVariable Long caseId,
            @PathVariable String documentType) {
        try {
            assertCaseVisible(caseId);
            List<CaseDocumentDTO> documents = caseDocumentService.getDocumentsByCaseAndType(caseId, documentType);
            return Result.success(documents);
        } catch (Exception e) {
            log.error("按类型获取文档列表异常", e);
            return Result.error("获取文档列表失败");
        }
    }

    /**
     * 获取文档详情
     * GET /api/cases/{caseId}/documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<CaseDocumentDTO> getDocument(
            @PathVariable Long caseId,
            @PathVariable Long id) {
        try {
            assertCaseVisible(caseId);
            CaseDocumentDTO document = caseDocumentService.getDocumentById(id);
            assertDocumentInCase(document, caseId);
            return Result.success(document);
        } catch (IllegalArgumentException e) {
            log.error("获取文档详情失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取文档详情异常", e);
            return Result.error("获取文档详情失败");
        }
    }

    /**
     * 更新文档信息
     * PUT /api/cases/{caseId}/documents/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CaseDocumentDTO> updateDocument(
            @PathVariable Long caseId,
            @PathVariable Long id,
            @RequestBody CaseDocumentDTO dto) {
        try {
            assertCaseVisible(caseId);
            assertDocumentInCase(caseDocumentService.getDocumentById(id), caseId);
            CaseDocumentDTO result = caseDocumentService.updateDocument(id, dto);
            return Result.success("文档更新成功", result);
        } catch (IllegalArgumentException e) {
            log.error("更新文档失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新文档异常", e);
            return Result.error("更新文档失败");
        }
    }

    /**
     * 移动文档到其他文件夹
     * PUT /api/cases/{caseId}/documents/{id}/move
     */
    @PutMapping("/{id}/move")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CaseDocumentDTO> moveDocument(
            @PathVariable Long caseId,
            @PathVariable Long id,
            @RequestParam String folderPath) {
        try {
            assertCaseVisible(caseId);
            assertDocumentInCase(caseDocumentService.getDocumentById(id), caseId);
            CaseDocumentDTO result = caseDocumentService.moveDocument(id, folderPath);
            return Result.success("文档移动成功", result);
        } catch (IllegalArgumentException e) {
            log.error("移动文档失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("移动文档异常", e);
            return Result.error("移动文档失败");
        }
    }

    /**
     * 删除文档
     * DELETE /api/cases/{caseId}/documents/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deleteDocument(
            @PathVariable Long caseId,
            @PathVariable Long id) {
        try {
            assertCaseVisible(caseId);
            assertDocumentInCase(caseDocumentService.getDocumentById(id), caseId);
            caseDocumentService.deleteDocument(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除文档失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除文档异常", e);
            return Result.error("删除文档失败");
        }
    }

    /**
     * 下载文档
     * GET /api/cases/{caseId}/documents/{id}/download
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public void downloadDocument(
            @PathVariable Long caseId,
            @PathVariable Long id,
            HttpServletResponse response) throws IOException {
        try {
            assertCaseVisible(caseId);
            CaseDocumentDTO document = caseDocumentService.getDocumentById(id);
            assertDocumentInCase(document, caseId);

            if (document.getFilePath() == null || document.getFilePath().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件路径不存在");
                return;
            }

            Path filePath = Paths.get(document.getFilePath());
            if (!java.nio.file.Files.exists(filePath)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件不存在");
                return;
            }

            Resource resource = new UrlResource(filePath.toUri());

            String contentType = null;
            try {
                contentType = java.nio.file.Files.probeContentType(filePath);
            } catch (IOException e) {
                contentType = "application/octet-stream";
            }

            response.setContentType(contentType != null ? contentType : "application/octet-stream");
            String encodedFilename = URLEncoder.encode(document.getDocumentName(), StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encodedFilename);

            java.io.InputStream inputStream = resource.getInputStream();
            org.springframework.util.StreamUtils.copy(inputStream, response.getOutputStream());

            response.flushBuffer();
        } catch (Exception e) {
            log.error("下载文档失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("下载失败: " + e.getMessage());
        }
    }

    private void assertCaseVisible(Long caseId) {
        Long userId = securityUtils.getCurrentUserId();
        caseService.assertCaseVisible(caseId, userId);
    }

    private void assertDocumentInCase(CaseDocumentDTO document, Long caseId) {
        if (document.getCaseId() == null || !document.getCaseId().equals(caseId)) {
            throw new IllegalArgumentException("文档不属于当前案件");
        }
    }
}
