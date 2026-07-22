package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.LegacyMaterialSearchRequest;
import com.lawfirm.dto.LegacyMaterialSearchResponse;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.LegacyMaterialSearchService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;

/**
 * 旧系统资料检索。
 */
@Slf4j
@RestController
@RequestMapping("/legacy-materials")
@RequiredArgsConstructor
public class LegacyMaterialSearchController {

    private final LegacyMaterialSearchService legacyMaterialSearchService;
    private final SecurityUtils securityUtils;

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    @AuditLog(value = "检索旧案材料", operationType = "SEARCH", logParams = false)
    public Result<LegacyMaterialSearchResponse> search(@Valid @RequestBody LegacyMaterialSearchRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return Result.success(legacyMaterialSearchService.search(request, currentUserId));
    }

    @GetMapping("/files/{resultId}/download")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    @AuditLog(value = "下载旧案材料", operationType = "DOWNLOAD", logParams = false)
    public ResponseEntity<FileSystemResource> download(@PathVariable Long resultId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        LegacyMaterialSearchService.FileDownload download =
                legacyMaterialSearchService.loadDownload(resultId, currentUserId);
        FileSystemResource resource = new FileSystemResource(download.getPath());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
