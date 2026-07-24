package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.LawFirmLetterDTO;
import com.lawfirm.dto.LawFirmLetterUpdateRequest;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.LawFirmLetterService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class LawFirmLetterController {
    private final LawFirmLetterService service;
    private final SecurityUtils securityUtils;

    @GetMapping("cases/{caseId}/law-firm-letters")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<LawFirmLetterDTO>> list(@PathVariable Long caseId) {
        return Result.success(service.list(caseId, securityUtils.getCurrentUserId()));
    }

    @PostMapping("cases/{caseId}/law-firm-letters")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "创建律所所函", operationType = "CREATE", logParams = false)
    public Result<LawFirmLetterDTO> create(@PathVariable Long caseId) {
        return Result.success(service.create(caseId, securityUtils.getCurrentUserId()));
    }

    @GetMapping("law-firm-letters/{id}")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<LawFirmLetterDTO> get(@PathVariable Long id) {
        return Result.success(service.get(id, securityUtils.getCurrentUserId()));
    }

    @PutMapping("law-firm-letters/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "修改律所所函", operationType = "UPDATE", logParams = false)
    public Result<LawFirmLetterDTO> update(@PathVariable Long id,
                                           @Valid @RequestBody LawFirmLetterUpdateRequest request) {
        return Result.success(service.update(id, request, securityUtils.getCurrentUserId()));
    }

    @DeleteMapping("law-firm-letters/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "取消律所所函草稿", operationType = "DELETE", logParams = false)
    public Result<Void> cancel(@PathVariable Long id) {
        service.cancel(id, securityUtils.getCurrentUserId());
        return Result.success();
    }

    @PostMapping("law-firm-letters/{id}/submit")
    @PreAuthorize("hasAuthority('CASE_EDIT') and hasAuthority('APPROVAL_EDIT')")
    @AuditLog(value = "提交律所所函用印审批", operationType = "SUBMIT", logParams = false)
    public Result<LawFirmLetterDTO> submit(@PathVariable Long id) {
        return Result.success(service.submit(id, securityUtils.getCurrentUserId()));
    }

    @GetMapping("law-firm-letters/{id}/docx")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    @AuditLog(value = "下载律所所函", operationType = "DOWNLOAD", logParams = false)
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        LawFirmLetterService.GeneratedLetterFile file = service.download(id, securityUtils.getCurrentUserId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.getContent());
    }
}
