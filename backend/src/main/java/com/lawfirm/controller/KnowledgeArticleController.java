package com.lawfirm.controller;

import com.lawfirm.dto.KnowledgeArticleDTO;
import com.lawfirm.dto.KnowledgeArticleVO;
import com.lawfirm.service.KnowledgeArticleService;
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

import java.util.List;

/**
 * 知识库文章Controller
 */
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库文章的增删改查接口")
public class KnowledgeArticleController {

    private final KnowledgeArticleService articleService;

    @PostMapping
    @Operation(summary = "创建文章")
    public Result<KnowledgeArticleVO> createArticle(@Valid @RequestBody KnowledgeArticleDTO dto) {
        KnowledgeArticleVO vo = articleService.createArticle(dto);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文章")
    public Result<KnowledgeArticleVO> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeArticleDTO dto) {
        KnowledgeArticleVO vo = articleService.updateArticle(id, dto);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
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
    @Operation(summary = "点赞文章")
    public Result<Void> likeArticle(@PathVariable Long id) {
        articleService.likeArticle(id);
        return Result.success();
    }

    @GetMapping("/type/{articleType}")
    @Operation(summary = "根据类型查询文章")
    public Result<Page<KnowledgeArticleVO>> listByType(
            @PathVariable String articleType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KnowledgeArticleVO> result = articleService.listByType(articleType, pageable);
        return Result.success(result);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "根据分类查询文章")
    public Result<Page<KnowledgeArticleVO>> listByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KnowledgeArticleVO> result = articleService.listByCategory(category, pageable);
        return Result.success(result);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索文章")
    public Result<Page<KnowledgeArticleVO>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KnowledgeArticleVO> result = articleService.searchArticles(keyword, pageable);
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KnowledgeArticleVO> result = articleService.getMyArticles(pageable);
        return Result.success(result);
    }
}
