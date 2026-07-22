package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.KnowledgeArticleDTO;
import com.lawfirm.dto.KnowledgeArticleVO;
import com.lawfirm.service.KnowledgeArticleService;
import com.lawfirm.service.KnowledgeDocumentImportService;
import com.lawfirm.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.time.LocalDate;

/**
 * 知识库文章Controller
 */
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库文章的增删改查接口")
public class KnowledgeArticleController {

    private final KnowledgeArticleService articleService;
    private final KnowledgeDocumentImportService documentImportService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "创建知识文章", operationType = "CREATE", logParams = false)
    @Operation(summary = "创建文章")
    public Result<KnowledgeArticleVO> createArticle(@Valid @RequestBody KnowledgeArticleDTO dto) {
        KnowledgeArticleVO vo = articleService.createArticle(dto);
        return Result.success(vo);
    }

    @PostMapping("/import")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "导入知识文档", operationType = "UPLOAD", logParams = false)
    @Operation(summary = "导入知识文档")
    public Result<KnowledgeArticleVO> importDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeSource") String knowledgeSource,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "DOCUMENT") String articleType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String sourceReference,
            @RequestParam(required = false) String issuingAuthority,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) LocalDate effectiveDate,
            @RequestParam(defaultValue = "UNKNOWN") String validityStatus,
            @RequestParam(defaultValue = "false") Boolean authorizationConfirmed,
            @RequestParam(defaultValue = "true") Boolean isPublic,
            @RequestParam(defaultValue = "true") Boolean knowledgeEligible) {
        KnowledgeArticleDTO dto = new KnowledgeArticleDTO();
        dto.setTitle(title);
        dto.setArticleType(articleType);
        dto.setKnowledgeSource(knowledgeSource);
        dto.setCategory(category);
        dto.setTags(tags);
        dto.setSummary(summary);
        dto.setSourceReference(sourceReference);
        dto.setIssuingAuthority(issuingAuthority);
        dto.setDocumentNumber(documentNumber);
        dto.setEffectiveDate(effectiveDate);
        dto.setValidityStatus(validityStatus);
        dto.setAuthorizationConfirmed(authorizationConfirmed);
        dto.setIsPublic(isPublic);
        dto.setKnowledgeEligible(knowledgeEligible);
        dto.setIsTop(false);
        return Result.success("知识文档导入成功", documentImportService.importDocument(file, dto));
    }

    @GetMapping("/{id}/attachment")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "下载知识文档原件")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        return documentImportService.downloadAttachment(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "更新知识文章", operationType = "UPDATE", logParams = false)
    @Operation(summary = "更新文章")
    public Result<KnowledgeArticleVO> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeArticleDTO dto) {
        KnowledgeArticleVO vo = articleService.updateArticle(id, dto);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "删除知识文章", operationType = "DELETE", logParams = false)
    @Operation(summary = "删除文章")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "分页查询文章")
    public Result<Page<KnowledgeArticleVO>> listArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = knowledgePage(page, size);
        Page<KnowledgeArticleVO> result = articleService.listArticles(pageable);
        return Result.success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询文章（别名）")
    public Result<Page<KnowledgeArticleVO>> listArticlesAlias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return listArticles(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文章详情")
    public Result<KnowledgeArticleVO> getArticle(@PathVariable Long id) {
        KnowledgeArticleVO vo = articleService.getArticle(id);
        return Result.success(vo);
    }

    @PostMapping("/{id}/like")
    @AuditLog(value = "点赞知识文章", operationType = "LIKE", logParams = false)
    @Operation(summary = "点赞文章")
    public Result<Void> likeArticle(@PathVariable Long id) {
        articleService.likeArticle(id);
        return Result.success();
    }

    @PostMapping("/{id}/reindex")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "重新索引知识文章", operationType = "REINDEX", logParams = false)
    @Operation(summary = "重新索引知识文章")
    public Result<KnowledgeArticleVO> reindexArticle(@PathVariable Long id) {
        return Result.success("索引状态已刷新", articleService.reindexArticle(id));
    }

    @GetMapping("/type/{articleType}")
    @Operation(summary = "根据类型查询文章")
    public Result<Page<KnowledgeArticleVO>> listByType(
            @PathVariable String articleType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = knowledgePage(page, size);
        Page<KnowledgeArticleVO> result = articleService.listByType(articleType, pageable);
        return Result.success(result);
    }

    @GetMapping("/source/{source}")
    @Operation(summary = "根据知识来源查询文章")
    public Result<Page<KnowledgeArticleVO>> listBySource(
            @PathVariable String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<KnowledgeArticleVO> result = articleService.listBySource(source, knowledgePage(page, size));
        return Result.success(result);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "根据分类查询文章")
    public Result<Page<KnowledgeArticleVO>> listByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = knowledgePage(page, size);
        Page<KnowledgeArticleVO> result = articleService.listByCategory(category, pageable);
        return Result.success(result);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索文章")
    public Result<Page<KnowledgeArticleVO>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = knowledgePage(page, size);
        Page<KnowledgeArticleVO> result = articleService.searchArticles(keyword, source, pageable);
        return Result.success(result);
    }

    @GetMapping("/top")
    @Operation(summary = "获取置顶文章")
    public Result<List<KnowledgeArticleVO>> getTopArticles() {
        List<KnowledgeArticleVO> result = articleService.getTopArticles();
        return Result.success(result);
    }

    @GetMapping("/hot")
    @Operation(summary = "获取热门文章")
    public Result<List<KnowledgeArticleVO>> getHotArticles() {
        List<KnowledgeArticleVO> result = articleService.getHotArticles();
        return Result.success(result);
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新文章")
    public Result<List<KnowledgeArticleVO>> getLatestArticles() {
        List<KnowledgeArticleVO> result = articleService.getLatestArticles();
        return Result.success(result);
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的文章")
    public Result<Page<KnowledgeArticleVO>> getMyArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = knowledgePage(page, size);
        Page<KnowledgeArticleVO> result = articleService.getMyArticles(pageable);
        return Result.success(result);
    }

    private Pageable knowledgePage(int page, int size) {
        return PageRequest.of(Math.max(0, page), size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
