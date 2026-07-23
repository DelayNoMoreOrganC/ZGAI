package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.*;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.ArchiveWorkflowService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ArchiveWorkflowController {
    private final ArchiveWorkflowService archiveWorkflowService;
    private final SecurityUtils securityUtils;

    @GetMapping("cases/{caseId}/archive-readiness")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<ArchiveReadinessDTO> readiness(@PathVariable Long caseId) {
        return Result.success(archiveWorkflowService.getReadiness(caseId, securityUtils.getCurrentUserId()));
    }

    @PostMapping("cases/{caseId}/archive-jobs")
    @PreAuthorize("hasAuthority('CASE_ARCHIVE')")
    @AuditLog(value = "发起智能归档", operationType = "CREATE", logParams = false)
    public Result<ArchiveJobDTO> create(@PathVariable Long caseId,
                                        @Valid @RequestBody(required = false) ArchiveJobCreateRequest request) {
        return Result.success("智能归档任务已创建",
                archiveWorkflowService.createJob(caseId, request, securityUtils.getCurrentUserId()));
    }

    @GetMapping("archive-jobs")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<ArchiveJobDTO>> list(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long caseId) {
        return Result.success(archiveWorkflowService.listJobs(status, caseId, securityUtils.getCurrentUserId()));
    }

    @GetMapping("archive-jobs/{jobId}")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<ArchiveJobDTO> detail(@PathVariable Long jobId) {
        return Result.success(archiveWorkflowService.getJob(jobId, securityUtils.getCurrentUserId()));
    }

    @PatchMapping("archive-jobs/{jobId}/documents")
    @PreAuthorize("hasAuthority('CASE_ARCHIVE')")
    @AuditLog(value = "核对归档材料", operationType = "UPDATE", logParams = false)
    public Result<ArchiveJobDTO> patchDocuments(@PathVariable Long jobId,
                                                 @Valid @RequestBody ArchiveDocumentPatchRequest request) {
        return Result.success(archiveWorkflowService.patchDocuments(jobId, request, securityUtils.getCurrentUserId()));
    }

    @PostMapping("archive-jobs/{jobId}/documents")
    @PreAuthorize("hasAuthority('CASE_ARCHIVE')")
    @AuditLog(value = "补传归档材料", operationType = "UPLOAD", logParams = false)
    public Result<ArchiveJobDTO> uploadSupplement(@PathVariable Long jobId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam Integer catalogSeq) {
        return Result.success("补充材料已加入归档任务",
                archiveWorkflowService.uploadSupplement(jobId, file, catalogSeq, securityUtils.getCurrentUserId()));
    }

    @PatchMapping("archive-jobs/{jobId}/fields")
    @PreAuthorize("hasAuthority('CASE_ARCHIVE')")
    @AuditLog(value = "核对归档字段", operationType = "UPDATE", logParams = false)
    public Result<ArchiveJobDTO> patchFields(@PathVariable Long jobId,
                                             @Valid @RequestBody ArchiveFieldsPatchRequest request) {
        return Result.success(archiveWorkflowService.patchFields(jobId, request, securityUtils.getCurrentUserId()));
    }

    @PostMapping("archive-jobs/{jobId}/submit")
    @PreAuthorize("hasAuthority('CASE_ARCHIVE')")
    @AuditLog(value = "提交归档复核", operationType = "SUBMIT", logParams = false)
    public Result<ArchiveJobDTO> submit(@PathVariable Long jobId) {
        return Result.success("已提交行政复核",
                archiveWorkflowService.submit(jobId, securityUtils.getCurrentUserId()));
    }

    @PostMapping("archive-jobs/{jobId}/review")
    @PreAuthorize("hasAuthority('CASE_ARCHIVE_REVIEW')")
    @AuditLog(value = "行政复核归档", operationType = "APPROVE", logParams = false)
    public Result<ArchiveJobDTO> review(@PathVariable Long jobId,
                                        @Valid @RequestBody ArchiveReviewRequest request) {
        return Result.success(archiveWorkflowService.review(jobId, request, securityUtils.getCurrentUserId()));
    }

    @GetMapping("archive-jobs/{jobId}/download")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public org.springframework.http.ResponseEntity<Resource> download(@PathVariable Long jobId) throws Exception {
        Path path = archiveWorkflowService.getOutputPath(jobId, securityUtils.getCurrentUserId());
        Resource resource = new UrlResource(path.toUri());
        String encoded = URLEncoder.encode(path.getFileName().toString(), StandardCharsets.UTF_8).replace("+", "%20");
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }

    @GetMapping("archive-jobs/{jobId}/preview")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public org.springframework.http.ResponseEntity<Resource> preview(@PathVariable Long jobId) throws Exception {
        Path path = archiveWorkflowService.getPreviewPath(jobId, securityUtils.getCurrentUserId());
        Resource resource = new UrlResource(path.toUri());
        String encoded = URLEncoder.encode(path.getFileName().toString(), StandardCharsets.UTF_8).replace("+", "%20");
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
