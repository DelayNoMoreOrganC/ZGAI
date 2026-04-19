package com.lawfirm.controller;

import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.service.CaseDocumentService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

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

    /**
     * 获取全部案件文档（聚合视图）
     * GET /api/documents
     * 返回当前用户有权限访问的所有案件的文档
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<List<CaseDocumentDTO>> getAllDocuments(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) Long caseId) {
        try {
            List<CaseDocumentDTO> documents;

            if (caseId != null) {
                // 查询指定案件的文档
                documents = caseDocumentService.getCaseDocuments(caseId);
            } else if (documentType != null) {
                // 按类型查询所有文档
                documents = caseDocumentService.getDocumentsByType(documentType);
            } else {
                // 查询全部文档（这里需要添加新的Service方法）
                documents = caseDocumentService.getAllDocuments();
            }

            log.info("获取全部文档列表成功: count={}", documents.size());
            return Result.success(documents);
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取文档详情（兼容接口）
     * GET /api/documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<CaseDocumentDTO> getDocument(@PathVariable Long id) {
        try {
            CaseDocumentDTO result = caseDocumentService.getDocumentById(id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取文档详情失败", e);
            return Result.error(e.getMessage());
        }
    }
}
