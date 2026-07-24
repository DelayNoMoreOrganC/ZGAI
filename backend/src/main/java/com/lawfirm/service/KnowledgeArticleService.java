package com.lawfirm.service;

import com.lawfirm.dto.KnowledgeArticleDTO;
import com.lawfirm.dto.KnowledgeArticleVO;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.mapper.KnowledgeArticleMapper;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文章Service - 使用MyBatis实现
 * 绕过Hibernate ORM解决表名映射问题
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeArticleService {

    private final KnowledgeArticleMapper articleMapper;
    private final VectorMigrationService vectorMigrationService;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    /**
     * 创建文章
     */
    @Transactional
    public KnowledgeArticleVO createArticle(KnowledgeArticleDTO dto) {
        return createArticleInternal(dto, null);
    }

    @Transactional
    public KnowledgeArticleVO createImportedArticle(KnowledgeArticleDTO dto, String attachmentPath) {
        return createArticleInternal(dto, attachmentPath);
    }

    private KnowledgeArticleVO createArticleInternal(KnowledgeArticleDTO dto, String attachmentPath) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(dto.getTitle());
        article.setArticleType(dto.getArticleType());
        article.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true);
        applyKnowledgeMetadata(article, dto);
        applyKnowledgeScope(article, dto);
        article.setCategory(dto.getCategory());
        article.setTags(dto.getTags());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setAttachmentPath(attachmentPath);
        applyReviewGate(article, null);
        article.setIsTop(dto.getIsTop() != null ? dto.getIsTop() : false);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setDeleted(false);

        // 设置作者
        Long userId = securityUtils.getCurrentUserId();
        String username = getCurrentUserName(userId);
        article.setAuthorId(userId);
        article.setAuthorName(username);

        // 设置时间戳
        LocalDateTime now = LocalDateTime.now();
        article.setCreatedAt(now);
        article.setUpdatedAt(now);

        articleMapper.insert(article);
        vectorMigrationService.indexNewArticle(article);
        log.info("创建知识库文章成功: id={}, title={}", article.getId(), article.getTitle());
        return toVO(article);
    }

    /**
     * 更新文章
     */
    @Transactional
    public KnowledgeArticleVO updateArticle(Long id, KnowledgeArticleDTO dto) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        assertCanModify(article);

        String previousSource = article.getKnowledgeSource();
        article.setTitle(dto.getTitle());
        article.setArticleType(dto.getArticleType());
        article.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true);
        applyKnowledgeMetadata(article, dto);
        applyKnowledgeScope(article, dto);
        article.setCategory(dto.getCategory());
        article.setTags(dto.getTags());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setIsTop(dto.getIsTop());
        applyReviewGate(article, previousSource);

        // 更新修改人和时间
        article.setUpdaterId(securityUtils.getCurrentUserId());
        article.setUpdatedAt(LocalDateTime.now());

        articleMapper.update(article);
        vectorMigrationService.indexNewArticle(article);
        log.info("更新知识库文章成功: id={}, title={}", article.getId(), article.getTitle());
        return toVO(article);
    }

    /**
     * 删除文章（逻辑删除）
     */
    @Transactional
    public void deleteArticle(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        assertCanModify(article);
        articleMapper.deleteById(id, LocalDateTime.now());
        vectorMigrationService.deleteArticleIndex(id);
        log.info("删除知识库文章成功: id={}", id);
    }

    /**
     * 获取文章详情
     */
    @Transactional(readOnly = true)
    public KnowledgeArticleVO getArticle(Long id) {
        KnowledgeArticle article = getAuthorizedArticle(id);

        // 增加浏览次数
        articleMapper.incrementViewCount(id);
        article.setViewCount(article.getViewCount() + 1);

        return toVO(article);
    }

    @Transactional(readOnly = true)
    public KnowledgeArticle getArticleForAttachment(Long id) {
        return getAuthorizedArticle(id);
    }

    /**
     * 点赞文章
     */
    @Transactional
    public void likeArticle(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        assertCanView(article);
        articleMapper.incrementLikeCount(id);
        log.info("点赞文章成功: id={}", id);
    }

    @Transactional
    public KnowledgeArticleVO reindexArticle(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        assertCanModify(article);
        vectorMigrationService.indexNewArticle(article);
        KnowledgeArticle refreshed = articleMapper.selectById(id);
        return toVO(refreshed != null ? refreshed : article);
    }

    /**
     * 分页查询文章
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeArticleVO> listArticles(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();

        List<KnowledgeArticle> articles = articleMapper.selectByPage(offset, pageSize);
        int total = articleMapper.countTotal();

        return new PageImpl<>(
                articles.stream().map(this::toVO).collect(Collectors.toList()),
                pageable,
                total
        );
    }

    /**
     * 根据类型查询
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeArticleVO> listByType(String articleType, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();

        List<KnowledgeArticle> articles = articleMapper.selectByTypeAndPage(articleType, offset, pageSize);
        int total = articleMapper.countByType(articleType);

        return new PageImpl<>(
                articles.stream().map(this::toVO).collect(Collectors.toList()),
                pageable,
                total
        );
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeArticleVO> listBySource(String source, Pageable pageable) {
        String normalizedSource = validateKnowledgeSource(source);
        if ("CASE_DEPOSIT".equals(normalizedSource)) {
            throw new RuntimeException("案件沉淀暂不在共享知识库开放");
        }
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();

        List<KnowledgeArticle> articles = articleMapper.selectBySourceAndPage(normalizedSource, offset, pageSize);
        int total = articleMapper.countBySource(normalizedSource);
        return new PageImpl<>(
                articles.stream().map(this::toVO).collect(Collectors.toList()),
                pageable,
                total
        );
    }

    /**
     * 根据分类查询
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeArticleVO> listByCategory(String category, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();

        List<KnowledgeArticle> articles = articleMapper.selectByCategoryAndPage(category, offset, pageSize);
        int total = articleMapper.countByCategory(category);

        return new PageImpl<>(
                articles.stream().map(this::toVO).collect(Collectors.toList()),
                pageable,
                total
        );
    }

    /**
     * 搜索文章
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeArticleVO> searchArticles(String keyword, String source, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedSource = source == null || source.trim().isEmpty()
                ? null
                : validateKnowledgeSource(source);
        if (KnowledgeArticlePolicy.CASE_DEPOSIT.equals(normalizedSource)) {
            throw new IllegalArgumentException("案件沉淀暂不在共享知识库开放");
        }

        List<KnowledgeArticle> articles = normalizedSource == null
                ? articleMapper.searchByKeyword(normalizedKeyword, offset, pageSize)
                : articleMapper.searchByKeywordAndSource(normalizedKeyword, normalizedSource, offset, pageSize);
        int total = normalizedSource == null
                ? articleMapper.countSearch(normalizedKeyword)
                : articleMapper.countSearchBySource(normalizedKeyword, normalizedSource);

        return new PageImpl<>(
                articles.stream().map(this::toVO).collect(Collectors.toList()),
                pageable,
                total
        );
    }

    /**
     * 获取置顶文章
     */
    @Transactional(readOnly = true)
    public List<KnowledgeArticleVO> getTopArticles() {
        return articleMapper.selectTopArticles()
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取热门文章
     */
    @Transactional(readOnly = true)
    public List<KnowledgeArticleVO> getHotArticles() {
        return articleMapper.selectHotArticles()
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取最新文章
     */
    @Transactional(readOnly = true)
    public List<KnowledgeArticleVO> getLatestArticles() {
        return articleMapper.selectLatestArticles()
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取我的文章
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeArticleVO> getMyArticles(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();

        List<KnowledgeArticle> articles = articleMapper.selectMyArticles(userId, offset, pageSize);
        int total = articleMapper.countMyArticles(userId);

        return new PageImpl<>(
                articles.stream().map(this::toVO).collect(Collectors.toList()),
                pageable,
                total
        );
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeArticleVO> getPendingReviewArticles(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        List<KnowledgeArticle> articles = articleMapper.selectPendingReview(offset, pageSize);
        int total = articleMapper.countPendingReview();
        return new PageImpl<>(
                articles.stream().map(this::toVO).collect(Collectors.toList()),
                pageable,
                total
        );
    }

    /**
     * Entity转VO
     */
    private KnowledgeArticleVO toVO(KnowledgeArticle entity) {
        KnowledgeArticleVO vo = new KnowledgeArticleVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setArticleType(entity.getArticleType());
        vo.setKnowledgeSource(entity.getKnowledgeSource());
        vo.setCategory(entity.getCategory());
        vo.setTags(entity.getTags());
        vo.setSummary(entity.getSummary());
        vo.setContent(entity.getContent());
        // 只向前端暴露附件是否存在，绝不暴露 NAS/本机绝对路径。
        vo.setAttachmentPath(entity.getAttachmentPath() == null ? null : "AVAILABLE");
        vo.setAttachmentName(resolveAttachmentName(entity.getAttachmentPath()));
        vo.setSourceReference(entity.getSourceReference());
        vo.setSourceUrl(entity.getSourceUrl());
        vo.setSourceRelativePath(entity.getSourceRelativePath());
        vo.setContentSha256(entity.getContentSha256());
        vo.setReviewStatus(entity.getReviewStatus());
        vo.setReviewedBy(entity.getReviewedBy());
        vo.setReviewedAt(entity.getReviewedAt());
        vo.setReviewReason(entity.getReviewReason());
        vo.setCollectedAt(entity.getCollectedAt());
        vo.setIssuingAuthority(entity.getIssuingAuthority());
        vo.setDocumentNumber(entity.getDocumentNumber());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setValidityStatus(KnowledgeArticlePolicy.normalizeValidityStatus(entity.getValidityStatus()));
        vo.setAuthorizationConfirmed(entity.getAuthorizationConfirmed());
        vo.setViewCount(entity.getViewCount());
        vo.setLikeCount(entity.getLikeCount());
        vo.setIsTop(entity.getIsTop());
        vo.setIsPublic(entity.getIsPublic());
        vo.setKnowledgeEligible(entity.getKnowledgeEligible());
        vo.setIndexStatus(entity.getIndexStatus());
        vo.setAuthorId(entity.getAuthorId());
        vo.setAuthorName(entity.getAuthorName());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private void applyKnowledgeScope(KnowledgeArticle article, KnowledgeArticleDTO dto) {
        String source = normalizeKnowledgeSource(dto.getKnowledgeSource(), dto.getArticleType());
        article.setKnowledgeSource(source);
        boolean requested = dto.getKnowledgeEligible() == null || dto.getKnowledgeEligible();
        article.setKnowledgeEligible(requested);
        if (!KnowledgeArticlePolicy.isRagIndexable(article)) {
            article.setKnowledgeEligible(false);
        }
        if (!article.getKnowledgeEligible()) {
            article.setIndexStatus("FORBIDDEN");
        } else {
            article.setIndexStatus("PENDING");
        }
    }

    private void applyKnowledgeMetadata(KnowledgeArticle article, KnowledgeArticleDTO dto) {
        article.setSourceReference(trimToNull(dto.getSourceReference()));
        article.setIssuingAuthority(trimToNull(dto.getIssuingAuthority()));
        article.setDocumentNumber(trimToNull(dto.getDocumentNumber()));
        article.setEffectiveDate(dto.getEffectiveDate());
        article.setValidityStatus(KnowledgeArticlePolicy.normalizeValidityStatus(dto.getValidityStatus()));
        article.setAuthorizationConfirmed(Boolean.TRUE.equals(dto.getAuthorizationConfirmed()));
    }

    private void applyReviewGate(KnowledgeArticle article, String previousSource) {
        boolean requiresReview = KnowledgeArticlePolicy.requiresReview(article.getKnowledgeSource())
                || KnowledgeArticlePolicy.requiresReview(previousSource)
                || !securityUtils.hasAuthority("KNOWLEDGE_MANAGE");
        if (!requiresReview) return;
        article.setReviewStatus("PENDING_REVIEW");
        article.setReviewedBy(null);
        article.setReviewedAt(null);
        article.setReviewReason(null);
        article.setIsPublic(false);
        article.setKnowledgeEligible(false);
        article.setIndexStatus("FORBIDDEN");
    }

    private String normalizeKnowledgeSource(String source, String articleType) {
        if (source != null && !source.trim().isEmpty()) {
            return validateKnowledgeSource(source);
        }
        if ("TEMPLATE".equals(articleType)) {
            return "PUBLIC_TEMPLATE";
        }
        if ("CASE".equals(articleType)) {
            return "CASE_DEPOSIT";
        }
        return "FIRM_KNOWLEDGE";
    }

    private String validateKnowledgeSource(String source) {
        return KnowledgeArticlePolicy.normalizeSource(source);
    }

    private void assertCanModify(KnowledgeArticle article) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId.equals(article.getAuthorId()) || securityUtils.isAdmin()) {
            return;
        }
        throw new AccessDeniedException("无权修改他人的知识库文章");
    }

    private void assertCanView(KnowledgeArticle article) {
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean sharedKnowledge = Boolean.TRUE.equals(article.getIsPublic())
                && !"CASE_DEPOSIT".equals(article.getKnowledgeSource());
        if (sharedKnowledge || currentUserId.equals(article.getAuthorId()) || securityUtils.isAdmin()
                || securityUtils.hasAuthority("KNOWLEDGE_MANAGE")) {
            return;
        }
        throw new AccessDeniedException("无权查看该知识库文章");
    }

    private KnowledgeArticle getAuthorizedArticle(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        assertCanView(article);
        return article;
    }

    private String resolveAttachmentName(String attachmentPath) {
        if (attachmentPath == null || attachmentPath.trim().isEmpty()) {
            return null;
        }
        String fileName = java.nio.file.Paths.get(attachmentPath).getFileName().toString();
        return fileName.replaceFirst("^[0-9a-fA-F-]{36}_", "");
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String getCurrentUserName(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRealName() == null ? user.getUsername() : user.getRealName())
                .orElse("未知用户");
    }
}
