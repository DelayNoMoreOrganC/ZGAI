package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.ClientDTO;
import com.lawfirm.dto.ConflictCheckRecordDTO;
import com.lawfirm.dto.ConflictCheckResultDTO;
import com.lawfirm.dto.ConflictCheckReviewRequest;
import com.lawfirm.dto.ConflictWaiverAttachmentDTO;
import com.lawfirm.dto.ClientSubjectRelationDTO;
import com.lawfirm.service.ClientService;
import com.lawfirm.service.ConflictWaiverAttachmentService;
import com.lawfirm.service.CaseService;
import com.lawfirm.service.ClientSubjectRelationService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 客户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final com.lawfirm.repository.UserRepository userRepository;
    private final ConflictWaiverAttachmentService conflictWaiverAttachmentService;
    private final CaseService caseService;
    private final ClientSubjectRelationService clientSubjectRelationService;

    @GetMapping("/{id}/relations")
    public Result<List<ClientSubjectRelationDTO>> getClientRelations(@PathVariable Long id) {
        try {
            clientService.getClientById(id, getCurrentUserId());
            return Result.success(clientSubjectRelationService.list(id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/relations")
    @PreAuthorize("hasAuthority('CLIENT_EDIT')")
    @AuditLog(value = "新增客户关联主体", operationType = "CREATE", logParams = false)
    public Result<ClientSubjectRelationDTO> createClientRelation(
            @PathVariable Long id,
            @RequestBody ClientSubjectRelationDTO request) {
        try {
            Long userId = getCurrentUserId();
            clientService.assertClientEditable(id, userId);
            if (request.getTargetClientId() != null) {
                clientService.getClientById(request.getTargetClientId(), userId);
            }
            return Result.success("关联主体已保存",
                    clientSubjectRelationService.create(id, request, userId));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/relations/{relationId}")
    @PreAuthorize("hasAuthority('CLIENT_EDIT')")
    @AuditLog(value = "删除客户关联主体", operationType = "DELETE", logParams = false)
    public Result<Void> deleteClientRelation(@PathVariable Long id, @PathVariable Long relationId) {
        try {
            clientService.assertClientEditable(id, getCurrentUserId());
            clientSubjectRelationService.delete(id, relationId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建客户
     * POST /api/clients
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_CREATE')")
    @AuditLog(value = "创建客户", operationType = "CREATE", logParams = false)
    public Result<ClientDTO> createClient(@Valid @RequestBody ClientDTO dto) {
        try {
            Long userId = getCurrentUserId();
            ClientDTO result = clientService.createClient(dto, userId);
            return Result.success("客户创建成功", result);
        } catch (IllegalArgumentException e) {
            log.error("创建客户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建客户异常", e);
            return Result.error("创建客户失败");
        }
    }

    /**
     * 更新客户
     * PUT /api/clients/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_EDIT')")
    @AuditLog(value = "更新客户", operationType = "UPDATE", logParams = false)
    public Result<ClientDTO> updateClient(@PathVariable Long id, @Valid @RequestBody ClientDTO dto) {
        try {
            clientService.assertClientEditable(id, getCurrentUserId());
            ClientDTO result = clientService.updateClient(id, dto);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("更新客户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新客户异常", e);
            return Result.error("更新客户失败");
        }
    }

    /**
     * 删除客户
     * DELETE /api/clients/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE')")
    @AuditLog(value = "删除客户", operationType = "DELETE", logParams = false)
    public Result<Void> deleteClient(@PathVariable Long id) {
        try {
            clientService.assertClientEditable(id, getCurrentUserId());
            clientService.deleteClient(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除客户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除客户异常", e);
            return Result.error("删除客户失败");
        }
    }

    /**
     * 查询客户详情
     * GET /api/clients/{id}
     */
    @GetMapping("/{id}")
    public Result<ClientDTO> getClient(@PathVariable Long id) {
        try {
            ClientDTO result = clientService.getClientById(id, getCurrentUserId());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("查询客户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询客户异常", e);
            return Result.error("查询客户失败");
        }
    }

    /**
     * 搜索客户
     * GET /api/clients/search?keyword={keyword}
     */
    @GetMapping("/search")
    public Result<List<ClientDTO>> searchClients(@RequestParam String keyword) {
        try {
            List<ClientDTO> result = clientService.searchClients(keyword, getCurrentUserId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("搜索客户异常", e);
            return Result.error("搜索客户失败");
        }
    }

    /**
     * 按类型查询客户
     * GET /api/clients/type/{clientType}
     */
    @GetMapping("/type/{clientType}")
    public Result<List<ClientDTO>> getClientsByType(@PathVariable String clientType) {
        try {
            List<ClientDTO> result = clientService.getClientsByType(clientType, getCurrentUserId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询客户异常", e);
            return Result.error("查询客户失败");
        }
    }

    /**
     * 按状态查询客户
     * GET /api/clients/status/{status}
     */
    @GetMapping("/status/{status}")
    public Result<List<ClientDTO>> getClientsByStatus(@PathVariable String status) {
        try {
            List<ClientDTO> result = clientService.getClientsByStatus(status, getCurrentUserId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询客户异常", e);
            return Result.error("查询客户失败");
        }
    }

    /**
     * 查询用户的客户
     * GET /api/clients/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    public Result<List<ClientDTO>> getClientsByOwner(@PathVariable Long ownerId) {
        try {
            List<ClientDTO> result = clientService.getClientsByOwner(ownerId, getCurrentUserId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询客户异常", e);
            return Result.error("查询客户失败");
        }
    }

    /**
     * 利益冲突检索
     * GET /api/clients/{id}/conflict-check
     */
    @GetMapping("/{id}/conflict-check")
    public Result<ClientDTO> checkConflict(@PathVariable Long id) {
        try {
            ClientDTO result = clientService.checkConflict(id, getCurrentUserId());
            return Result.success(result);
        } catch (Exception e) {
            log.error("利益冲突检索异常", e);
            return Result.error("利益冲突检索失败");
        }
    }

    /**
     * 客户建档前利益冲突预检
     * POST /api/clients/conflict-check
     */
    @PostMapping("/conflict-check")
    @AuditLog(value = "执行利冲检查", operationType = "CHECK", logParams = false)
    public Result<ConflictCheckResultDTO> checkConflictPreview(@RequestBody ClientDTO dto) {
        try {
            Long userId = getCurrentUserId();
            ConflictCheckResultDTO result = clientService.checkConflictPreviewAndRecord(dto, userId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("利益冲突预检异常", e);
            return Result.error("利益冲突预检失败");
        }
    }

    /**
     * 查询客户名称的利冲检查历史
     * GET /api/clients/conflict-check/records?subjectName={subjectName}
     */
    @GetMapping("/conflict-check/records")
    public Result<List<ConflictCheckRecordDTO>> getConflictCheckRecords(@RequestParam String subjectName) {
        try {
            return Result.success(clientService.getConflictCheckRecords(subjectName, getCurrentUserId()));
        } catch (Exception e) {
            log.error("查询利冲检查记录异常", e);
            return Result.error("查询利冲检查记录失败");
        }
    }

    /**
     * 行政管理提交正式利冲审查结论。正式结论提交后不可覆盖。
     */
    @PutMapping("/conflict-check/records/{id}/review")
    @PreAuthorize("hasAuthority('CASE_FILING_REVIEW')")
    @AuditLog(value = "提交正式利冲审查结论", operationType = "REVIEW", logParams = false)
    public Result<ConflictCheckRecordDTO> reviewConflictCheck(
            @PathVariable Long id,
            @RequestBody ConflictCheckReviewRequest request) {
        try {
            return Result.success("正式利冲审查已完成",
                    clientService.reviewConflictCheck(id, request, getCurrentUserId()));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("提交正式利冲审查异常", e);
            return Result.error("提交正式利冲审查失败");
        }
    }

    /**
     * 行政人员在正式审查锁定前上传书面豁免或风险处置依据原件。
     */
    @PostMapping("/conflict-check/records/{id}/waiver-attachments")
    @PreAuthorize("hasAuthority('CASE_FILING_REVIEW')")
    @AuditLog(value = "上传利冲豁免依据", operationType = "UPLOAD", logParams = false)
    public Result<ConflictWaiverAttachmentDTO> uploadConflictWaiverAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            return Result.success("豁免依据已上传",
                    conflictWaiverAttachmentService.upload(id, file, getCurrentUserId()));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("上传利冲豁免依据异常", e);
            return Result.error("上传利冲豁免依据失败");
        }
    }

    @GetMapping("/conflict-check/records/{id}/waiver-attachments")
    public Result<List<ConflictWaiverAttachmentDTO>> getConflictWaiverAttachments(@PathVariable Long id) {
        try {
            assertConflictRecordVisible(id);
            return Result.success(conflictWaiverAttachmentService.list(id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/conflict-check/records/{id}/waiver-attachments/{attachmentId}/download")
    @AuditLog(value = "下载利冲豁免依据", operationType = "DOWNLOAD", logParams = false)
    public ResponseEntity<Resource> downloadConflictWaiverAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId) throws Exception {
        assertConflictRecordVisible(id);
        ConflictWaiverAttachmentService.AttachmentDownload download =
                conflictWaiverAttachmentService.getDownload(id, attachmentId);
        Resource resource = new UrlResource(download.getPath().toUri());
        String filename = URLEncoder.encode(download.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(download.getMimeType()))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    private void assertConflictRecordVisible(Long recordId) {
        Long userId = getCurrentUserId();
        try {
            clientService.assertConflictRecordReadable(recordId, userId);
        } catch (IllegalArgumentException originalError) {
            Long caseId = clientService.getConflictRecordCaseId(recordId);
            if (caseId == null) {
                throw originalError;
            }
            caseService.assertCaseVisible(caseId, userId);
        }
    }

    /**
     * 下载利冲检查报告。报告只包含初筛摘要，不暴露无权客户或案件详情。
     */
    @GetMapping("/conflict-check/records/{id}/report")
    @AuditLog(value = "下载利冲检查报告", operationType = "DOWNLOAD", logParams = false)
    public ResponseEntity<byte[]> downloadConflictCheckReport(@PathVariable Long id) {
        String report = clientService.generateConflictCheckReport(id, getCurrentUserId());
        byte[] content = report.getBytes(StandardCharsets.UTF_8);
        String filename = URLEncoder.encode("利冲检查报告-" + id + ".txt", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .contentLength(content.length)
                .body(content);
    }

    /**
     * 获取客户的案件列表
     * GET /api/clients/{id}/cases
     */
    @GetMapping("/{id}/cases")
    public Result<List<com.lawfirm.vo.CaseListVO>> getClientCases(@PathVariable Long id) {
        try {
            List<com.lawfirm.vo.CaseListVO> cases = clientService.getClientCases(id, getCurrentUserId());
            return Result.success(cases);
        } catch (Exception e) {
            log.error("获取客户案件异常", e);
            return Result.error("获取客户案件失败");
        }
    }

    /**
     * 获取客户的沟通记录
     * GET /api/clients/{id}/communications
     */
    @GetMapping("/{id}/communications")
    public Result<List<com.lawfirm.entity.CommunicationRecord>> getCommunications(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<com.lawfirm.entity.CommunicationRecord> records = clientService.getCommunications(id, page, size, getCurrentUserId());
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取沟通记录异常", e);
            return Result.error("获取沟通记录失败");
        }
    }

    /**
     * 创建沟通记录
     * POST /api/clients/{id}/communications
     */
    @PostMapping("/{id}/communications")
    @PreAuthorize("hasAuthority('CLIENT_EDIT')")
    @AuditLog(value = "新增客户沟通记录", operationType = "CREATE", logParams = false)
    public Result<com.lawfirm.entity.CommunicationRecord> createCommunication(
            @PathVariable Long id,
            @Valid @RequestBody com.lawfirm.dto.CommunicationRecordDTO dto) {
        try {
            Long currentUserId = getCurrentUserId();
            com.lawfirm.entity.CommunicationRecord record = clientService.createCommunication(id, dto, currentUserId);
            return Result.success("沟通记录创建成功", record);
        } catch (Exception e) {
            log.error("创建沟通记录异常", e);
            return Result.error("创建沟通记录失败");
        }
    }

    /**
     * 更新沟通记录
     * PUT /api/clients/{id}/communications/{communicationId}
     */
    @PutMapping("/{id}/communications/{communicationId}")
    @PreAuthorize("hasAuthority('CLIENT_EDIT')")
    @AuditLog(value = "更新客户沟通记录", operationType = "UPDATE", logParams = false)
    public Result<com.lawfirm.entity.CommunicationRecord> updateCommunication(
            @PathVariable Long id,
            @PathVariable Long communicationId,
            @Valid @RequestBody com.lawfirm.dto.CommunicationRecordDTO dto) {
        try {
            com.lawfirm.entity.CommunicationRecord record = clientService.updateCommunication(id, communicationId, dto, getCurrentUserId());
            return Result.success("沟通记录更新成功", record);
        } catch (IllegalArgumentException e) {
            log.error("更新沟通记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新沟通记录异常", e);
            return Result.error("更新沟通记录失败");
        }
    }

    /**
     * 删除沟通记录
     * DELETE /api/clients/{id}/communications/{communicationId}
     */
    @DeleteMapping("/{id}/communications/{communicationId}")
    @PreAuthorize("hasAuthority('CLIENT_EDIT')")
    @AuditLog(value = "删除客户沟通记录", operationType = "DELETE", logParams = false)
    public Result<Void> deleteCommunication(
            @PathVariable Long id,
            @PathVariable Long communicationId) {
        try {
            clientService.deleteCommunication(id, communicationId, getCurrentUserId());
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除沟通记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除沟通记录异常", e);
            return Result.error("删除沟通记录失败");
        }
    }

    /**
     * 分页查询客户
     * GET /api/clients?page={page}&size={size}
     */
    @GetMapping
    public Result<com.lawfirm.util.PageResult<ClientDTO>> getClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String clientType,
            @RequestParam(required = false) Long departmentId) {
        try {
            var result = clientService.getClients(
                    page, size, getCurrentUserId(), keyword, clientType, departmentId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询客户异常", e);
            return Result.error("分页查询客户失败");
        }
    }

    /**
     * 从 Spring Security Context 中获取当前用户ID
     */
    private Long getCurrentUserId() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("用户未登录");
        }

        // JwtAuthenticationFilter 将 userId (Long类型) 设置为 principal
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        } else if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                // 如果是字符串格式的用户名，查询数据库
                return userRepository.findByUsername((String) principal)
                        .map(user -> user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("无效的用户信息"));
            }
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails =
                    (org.springframework.security.core.userdetails.UserDetails) principal;
            String username = userDetails.getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                return userRepository.findByUsername(username)
                        .map(user -> user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("无效的用户信息"));
            }
        }

        throw new IllegalArgumentException("无法识别的用户信息类型");
    }
}
