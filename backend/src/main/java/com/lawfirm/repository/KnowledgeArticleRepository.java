package com.lawfirm.repository;

import com.lawfirm.entity.KnowledgeArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库文章Repository
 */
@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long>, JpaSpecificationExecutor<KnowledgeArticle> {

    /**
     * 根据类型查询文章
     */
    Page<KnowledgeArticle> findByArticleTypeAndDeletedFalse(String articleType, Pageable pageable);

    /**
     * 根据分类查询文章
     */
    Page<KnowledgeArticle> findByCategoryAndDeletedFalse(String category, Pageable pageable);

    /**
     * 搜索文章（标题或内容包含关键词）
     */
    @Query("SELECT a FROM KnowledgeArticle a WHERE a.deleted = false AND " +
           "(a.title LIKE %:keyword% OR a.content LIKE %:keyword% OR a.tags LIKE %:keyword%)")
    Page<KnowledgeArticle> searchArticles(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查询置顶文章
     */
    List<KnowledgeArticle> findByIsTopTrueAndDeletedFalseOrderByCreatedAtDesc();

    /**
     * 查询热门文章（按浏览数排序）
     */
    List<KnowledgeArticle> findTop10ByDeletedFalseOrderByViewCountDesc();

    /**
     * 查询最新文章
     */
    List<KnowledgeArticle> findTop10ByDeletedFalseOrderByCreatedAtDesc();

    /**
     * 根据作者查询
     */
    Page<KnowledgeArticle> findByAuthorIdAndDeletedFalse(Long authorId, Pageable pageable);

    /**
     * 查询公开文章
     */
    @Query("SELECT a FROM KnowledgeArticle a WHERE a.deleted = false AND a.isPublic = true")
    Page<KnowledgeArticle> findPublicArticles(Pageable pageable);
}
