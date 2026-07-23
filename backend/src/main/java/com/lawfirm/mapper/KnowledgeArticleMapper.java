package com.lawfirm.mapper;

import com.lawfirm.dto.KnowledgeArticleVO;
import com.lawfirm.entity.KnowledgeArticle;
import org.apache.ibatis.annotations.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * KnowledgeArticle Mapper - 使用MyBatis原生SQL绕过Hibernate ORM
 * 解决Hibernate表名映射问题
 */
@Mapper
public interface KnowledgeArticleMapper {

    /**
     * 插入文章
     */
    @Insert("INSERT INTO knowledge_article " +
            "(title, article_type, knowledge_source, category, tags, summary, content, attachment_path, " +
            "source_reference, source_url, source_relative_path, content_sha256, collected_at, " +
            "issuing_authority, document_number, effective_date, validity_status, authorization_confirmed, " +
            "review_status, reviewed_by, reviewed_at, review_reason, knowledge_eligible, index_status, " +
            "view_count, like_count, is_top, is_public, author_id, author_name, updater_id, " +
            "created_at, updated_at, deleted) " +
            "VALUES " +
            "(#{title}, #{articleType}, #{knowledgeSource}, #{category}, #{tags}, #{summary}, #{content}, #{attachmentPath}, " +
            "#{sourceReference}, #{sourceUrl}, #{sourceRelativePath}, #{contentSha256}, #{collectedAt}, " +
            "#{issuingAuthority}, #{documentNumber}, #{effectiveDate}, #{validityStatus}, #{authorizationConfirmed}, " +
            "#{reviewStatus}, #{reviewedBy}, #{reviewedAt}, #{reviewReason}, #{knowledgeEligible}, #{indexStatus}, " +
            "#{viewCount}, #{likeCount}, #{isTop}, #{isPublic}, #{authorId}, #{authorName}, #{updaterId}, " +
            "#{createdAt}, #{updatedAt}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeArticle article);

    /**
     * 更新文章
     */
    @Update("UPDATE knowledge_article SET " +
            "title = #{title}, article_type = #{articleType}, knowledge_source = #{knowledgeSource}, " +
            "category = #{category}, tags = #{tags}, summary = #{summary}, content = #{content}, " +
            "attachment_path = #{attachmentPath}, knowledge_eligible = #{knowledgeEligible}, " +
            "source_reference = #{sourceReference}, source_url = #{sourceUrl}, " +
            "source_relative_path = #{sourceRelativePath}, content_sha256 = #{contentSha256}, collected_at = #{collectedAt}, " +
            "issuing_authority = #{issuingAuthority}, " +
            "document_number = #{documentNumber}, effective_date = #{effectiveDate}, " +
            "validity_status = #{validityStatus}, authorization_confirmed = #{authorizationConfirmed}, " +
            "review_status = #{reviewStatus}, reviewed_by = #{reviewedBy}, reviewed_at = #{reviewedAt}, " +
            "review_reason = #{reviewReason}, " +
            "index_status = #{indexStatus}, is_top = #{isTop}, is_public = #{isPublic}, " +
            "updater_id = #{updaterId}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND deleted = 0")
    int update(KnowledgeArticle article);

    /**
     * 逻辑删除文章
     */
    @Update("UPDATE knowledge_article SET deleted = 1, updated_at = #{updatedAt} WHERE id = #{id}")
    int deleteById(@Param("id") Long id, @Param("updatedAt") java.time.LocalDateTime updatedAt);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM knowledge_article WHERE id = #{id} AND deleted = 0")
    KnowledgeArticle selectById(@Param("id") Long id);

    /**
     * 浏览数+1
     */
    @Update("UPDATE knowledge_article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 点赞数+1
     */
    @Update("UPDATE knowledge_article SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 分页查询全部文章
     */
    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') " +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<KnowledgeArticle> selectByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计总数
     */
    @Select("SELECT COUNT(*) FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT')")
    int countTotal();

    /**
     * 根据类型分页查询
     */
    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') " +
            "AND article_type = #{articleType} " +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<KnowledgeArticle> selectByTypeAndPage(
            @Param("articleType") String articleType,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计指定类型的数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') " +
            "AND article_type = #{articleType}")
    int countByType(@Param("articleType") String articleType);

    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND knowledge_source = #{source} " +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<KnowledgeArticle> selectBySourceAndPage(
            @Param("source") String source,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    @Select("SELECT COUNT(*) FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND knowledge_source = #{source}")
    int countBySource(@Param("source") String source);

    /**
     * 根据分类分页查询
     */
    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') " +
            "AND category = #{category} " +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<KnowledgeArticle> selectByCategoryAndPage(
            @Param("category") String category,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计指定分类的数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') " +
            "AND category = #{category}")
    int countByCategory(@Param("category") String category);

    /**
     * 搜索文章（标题、内容、标签）
     */
    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') AND " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR " +
            "content LIKE CONCAT('%', #{keyword}, '%') OR " +
            "tags LIKE CONCAT('%', #{keyword}, '%') OR " +
            "summary LIKE CONCAT('%', #{keyword}, '%') OR " +
            "issuing_authority LIKE CONCAT('%', #{keyword}, '%') OR " +
            "document_number LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<KnowledgeArticle> searchByKeyword(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计搜索结果数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') AND " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR " +
            "content LIKE CONCAT('%', #{keyword}, '%') OR " +
            "tags LIKE CONCAT('%', #{keyword}, '%') OR " +
            "summary LIKE CONCAT('%', #{keyword}, '%') OR " +
            "issuing_authority LIKE CONCAT('%', #{keyword}, '%') OR " +
            "document_number LIKE CONCAT('%', #{keyword}, '%'))")
    int countSearch(@Param("keyword") String keyword);

    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND knowledge_source = #{source} AND " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR " +
            "content LIKE CONCAT('%', #{keyword}, '%') OR " +
            "tags LIKE CONCAT('%', #{keyword}, '%') OR " +
            "summary LIKE CONCAT('%', #{keyword}, '%') OR " +
            "issuing_authority LIKE CONCAT('%', #{keyword}, '%') OR " +
            "document_number LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<KnowledgeArticle> searchByKeywordAndSource(
            @Param("keyword") String keyword,
            @Param("source") String source,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    @Select("SELECT COUNT(*) FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND knowledge_source = #{source} AND " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR " +
            "content LIKE CONCAT('%', #{keyword}, '%') OR " +
            "tags LIKE CONCAT('%', #{keyword}, '%') OR " +
            "summary LIKE CONCAT('%', #{keyword}, '%') OR " +
            "issuing_authority LIKE CONCAT('%', #{keyword}, '%') OR " +
            "document_number LIKE CONCAT('%', #{keyword}, '%'))")
    int countSearchBySource(@Param("keyword") String keyword, @Param("source") String source);

    /**
     * 查询置顶文章
     */
    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') AND is_top = 1 " +
            "ORDER BY created_at DESC")
    List<KnowledgeArticle> selectTopArticles();

    /**
     * 查询热门文章（按浏览量）
     */
    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') " +
            "ORDER BY view_count DESC LIMIT 10")
    List<KnowledgeArticle> selectHotArticles();

    /**
     * 查询最新文章
     */
    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND is_public = 1 " +
            "AND (knowledge_source IS NULL OR knowledge_source <> 'CASE_DEPOSIT') " +
            "ORDER BY created_at DESC LIMIT 10")
    List<KnowledgeArticle> selectLatestArticles();

    /**
     * 查询我的文章
     */
    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND author_id = #{authorId} " +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<KnowledgeArticle> selectMyArticles(
            @Param("authorId") Long authorId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计我的文章数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_article WHERE deleted = 0 AND author_id = #{authorId}")
    int countMyArticles(@Param("authorId") Long authorId);

    @Select("SELECT * FROM knowledge_article WHERE deleted = 0 AND review_status = 'PENDING_REVIEW' " +
            "ORDER BY created_at ASC LIMIT #{pageSize} OFFSET #{offset}")
    List<KnowledgeArticle> selectPendingReview(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    @Select("SELECT COUNT(*) FROM knowledge_article WHERE deleted = 0 AND review_status = 'PENDING_REVIEW'")
    int countPendingReview();
}
