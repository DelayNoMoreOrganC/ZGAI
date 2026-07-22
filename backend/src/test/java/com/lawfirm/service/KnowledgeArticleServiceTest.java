package com.lawfirm.service;

import com.lawfirm.dto.KnowledgeArticleDTO;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.entity.User;
import com.lawfirm.mapper.KnowledgeArticleMapper;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeArticleServiceTest {

    private KnowledgeArticleMapper articleMapper;
    private VectorMigrationService vectorMigrationService;
    private SecurityUtils securityUtils;
    private UserRepository userRepository;
    private KnowledgeArticleService service;

    @BeforeEach
    void setUp() {
        articleMapper = mock(KnowledgeArticleMapper.class);
        vectorMigrationService = mock(VectorMigrationService.class);
        securityUtils = mock(SecurityUtils.class);
        userRepository = mock(UserRepository.class);
        service = new KnowledgeArticleService(articleMapper, vectorMigrationService, securityUtils, userRepository);
    }

    @Test
    void createUsesAuthenticatedEmployeeAsAuthor() {
        when(securityUtils.getCurrentUserId()).thenReturn(12L);
        when(userRepository.findById(12L)).thenReturn(Optional.of(user(12L, "律师账号", "张律师")));
        doAnswer(invocation -> {
            KnowledgeArticle article = invocation.getArgument(0);
            article.setId(31L);
            return 1;
        }).when(articleMapper).insert(any(KnowledgeArticle.class));

        service.createArticle(dto("LAW_REGULATION", true));

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).insert(captor.capture());
        assertEquals(12L, captor.getValue().getAuthorId());
        assertEquals("张律师", captor.getValue().getAuthorName());
        verify(vectorMigrationService).indexNewArticle(captor.getValue());
    }

    @Test
    void caseDepositIsNeverEligibleForRag() {
        when(securityUtils.getCurrentUserId()).thenReturn(12L);
        when(userRepository.findById(12L)).thenReturn(Optional.of(user(12L, "律师账号", "张律师")));
        doAnswer(invocation -> {
            ((KnowledgeArticle) invocation.getArgument(0)).setId(32L);
            return 1;
        }).when(articleMapper).insert(any(KnowledgeArticle.class));

        service.createArticle(dto("CASE_DEPOSIT", true));

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).insert(captor.capture());
        assertFalse(captor.getValue().getKnowledgeEligible());
        assertEquals("FORBIDDEN", captor.getValue().getIndexStatus());
    }

    @Test
    void unknownKnowledgeSourceIsRejected() {
        assertThrows(RuntimeException.class, () -> service.createArticle(dto("PRIVATE_CASE_FILE", true)));
        verify(articleMapper, never()).insert(any(KnowledgeArticle.class));
    }

    @Test
    void unauthorizedReferenceCanBeSavedButCannotEnterRag() {
        when(securityUtils.getCurrentUserId()).thenReturn(12L);
        when(userRepository.findById(12L)).thenReturn(Optional.of(user(12L, "律师账号", "张律师")));
        doAnswer(invocation -> {
            ((KnowledgeArticle) invocation.getArgument(0)).setId(33L);
            return 1;
        }).when(articleMapper).insert(any(KnowledgeArticle.class));
        KnowledgeArticleDTO dto = dto("REFERENCE_MATERIAL", true);
        dto.setAuthorizationConfirmed(false);

        service.createArticle(dto);

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);
        verify(articleMapper).insert(captor.capture());
        assertFalse(captor.getValue().getKnowledgeEligible());
        assertEquals("FORBIDDEN", captor.getValue().getIndexStatus());
    }

    @Test
    void unrelatedUserCannotUpdateArticle() {
        KnowledgeArticle article = article(41L, 12L, true, "LAW_REGULATION");
        when(articleMapper.selectById(41L)).thenReturn(article);
        when(securityUtils.getCurrentUserId()).thenReturn(13L);
        when(securityUtils.isAdmin()).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.updateArticle(41L, dto("LAW_REGULATION", true)));
        verify(articleMapper, never()).update(any(KnowledgeArticle.class));
    }

    @Test
    void authorCanUpdateOwnArticle() {
        KnowledgeArticle article = article(42L, 12L, true, "LAW_REGULATION");
        when(articleMapper.selectById(42L)).thenReturn(article);
        when(securityUtils.getCurrentUserId()).thenReturn(12L);

        service.updateArticle(42L, dto("FIRM_POLICY", true));

        verify(articleMapper).update(article);
        verify(vectorMigrationService).indexNewArticle(article);
    }

    @Test
    void privateArticleIsInvisibleToUnrelatedUser() {
        KnowledgeArticle article = article(43L, 12L, false, "FIRM_POLICY");
        when(articleMapper.selectById(43L)).thenReturn(article);
        when(securityUtils.getCurrentUserId()).thenReturn(13L);
        when(securityUtils.isAdmin()).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.getArticle(43L));
        verify(articleMapper, never()).incrementViewCount(43L);
    }

    @Test
    void sharedArticleCanBeReadByAnyAuthenticatedUser() {
        KnowledgeArticle article = article(44L, 12L, true, "LAW_REGULATION");
        when(articleMapper.selectById(44L)).thenReturn(article);
        when(securityUtils.getCurrentUserId()).thenReturn(13L);

        assertEquals(44L, service.getArticle(44L).getId());
        verify(articleMapper).incrementViewCount(44L);
    }

    @Test
    void deletingArticleAlsoRemovesVectorIndex() {
        KnowledgeArticle article = article(45L, 12L, true, "PUBLIC_TEMPLATE");
        when(articleMapper.selectById(45L)).thenReturn(article);
        when(securityUtils.getCurrentUserId()).thenReturn(12L);

        service.deleteArticle(45L);

        verify(articleMapper).deleteById(any(Long.class), any());
        verify(vectorMigrationService).deleteArticleIndex(45L);
    }

    @Test
    void authorCanRetryArticleIndexing() {
        KnowledgeArticle article = article(46L, 12L, true, "LAW_REGULATION");
        article.setIndexStatus("FAILED");
        when(articleMapper.selectById(46L)).thenReturn(article);
        when(securityUtils.getCurrentUserId()).thenReturn(12L);

        service.reindexArticle(46L);

        verify(vectorMigrationService).indexNewArticle(article);
    }

    private KnowledgeArticleDTO dto(String source, boolean isPublic) {
        KnowledgeArticleDTO dto = new KnowledgeArticleDTO();
        dto.setTitle("测试知识");
        dto.setArticleType("DOCUMENT");
        dto.setKnowledgeSource(source);
        dto.setContent("测试内容");
        dto.setIsPublic(isPublic);
        dto.setKnowledgeEligible(true);
        dto.setIsTop(false);
        return dto;
    }

    private KnowledgeArticle article(Long id, Long authorId, boolean isPublic, String source) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(id);
        article.setTitle("测试知识");
        article.setArticleType("DOCUMENT");
        article.setKnowledgeSource(source);
        article.setContent("测试内容");
        article.setAuthorId(authorId);
        article.setAuthorName("作者");
        article.setIsPublic(isPublic);
        article.setKnowledgeEligible(true);
        article.setViewCount(0);
        article.setLikeCount(0);
        return article;
    }

    private User user(Long id, String username, String realName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        return user;
    }
}
