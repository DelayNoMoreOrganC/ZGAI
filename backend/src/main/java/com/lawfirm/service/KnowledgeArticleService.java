package com.lawfirm.service;

import com.lawfirm.dto.KnowledgeArticleDTO;
import com.lawfirm.dto.KnowledgeArticleVO;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.mapper.KnowledgeArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    /**
     * 创建文章
     */
    @Transactional
    public KnowledgeArticleVO createArticle(KnowledgeArticleDTO dto) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(dto.getTitle());
        article.setArticleType(dto.getArticleType());
        article.setCategory(dto.getCategory());
        article.setTags(dto.getTags());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setAttachmentPath(dto.getAttachmentPath());
        article.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true);
        article.setIsTop(dto.getIsTop() != null ? dto.getIsTop() : false);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setDeleted(false);

        // 设置作者
        Long userId = getCurrentUserId();
        String username = getCurrentUsername();
        article.setAuthorId(userId);
        article.setAuthorName(username);

        // 设置时间戳
        LocalDateTime now = LocalDateTime.now();
        article.setCreatedAt(now);
        article.setUpdatedAt(now);

        articleMapper.insert(article);
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

        article.setTitle(dto.getTitle());
        article.setArticleType(dto.getArticleType());
        article.setCategory(dto.getCategory());
        article.setTags(dto.getTags());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setAttachmentPath(dto.getAttachmentPath());
        article.setIsPublic(dto.getIsPublic());
        article.setIsTop(dto.getIsTop());

        // 更新修改人和时间
        article.setUpdaterId(getCurrentUserId());
        article.setUpdatedAt(LocalDateTime.now());

        articleMapper.update(article);
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
        articleMapper.deleteById(id, LocalDateTime.now());
        log.info("删除知识库文章成功: id={}", id);
    }

    /**
     * 获取文章详情
     */
    @Transactional(readOnly = true)
    public KnowledgeArticleVO getArticle(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        // 增加浏览次数
        articleMapper.incrementViewCount(id);
        article.setViewCount(article.getViewCount() + 1);

        return toVO(article);
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
        articleMapper.incrementLikeCount(id);
        log.info("点赞文章成功: id={}", id);
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
    public Page<KnowledgeArticleVO> searchArticles(String keyword, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();

        List<KnowledgeArticle> articles = articleMapper.searchByKeyword(keyword, offset, pageSize);
        int total = articleMapper.countSearch(keyword);

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
        Long userId = getCurrentUserId();
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

    /**
     * Entity转VO
     */
    private KnowledgeArticleVO toVO(KnowledgeArticle entity) {
        KnowledgeArticleVO vo = new KnowledgeArticleVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setArticleType(entity.getArticleType());
        vo.setCategory(entity.getCategory());
        vo.setTags(entity.getTags());
        vo.setSummary(entity.getSummary());
        vo.setContent(entity.getContent());
        vo.setAttachmentPath(entity.getAttachmentPath());
        vo.setViewCount(entity.getViewCount());
        vo.setLikeCount(entity.getLikeCount());
        vo.setIsTop(entity.getIsTop());
        vo.setIsPublic(entity.getIsPublic());
        vo.setAuthorId(entity.getAuthorId());
        vo.setAuthorName(entity.getAuthorName());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        // 从SecurityContext获取用户ID
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            // 简化实现：从用户名解析ID（生产环境应从UserService获取）
            return 1L; // 临时返回固定值
        }
        return 1L;
    }

    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return "admin";
    }
}
