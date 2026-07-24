package com.lawfirm.service;

import com.lawfirm.dto.RagEvaluationCaseDTO;
import com.lawfirm.dto.RagEvaluationCaseRequest;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.entity.RagEvaluationCase;
import com.lawfirm.entity.RagEvaluationRun;
import com.lawfirm.exception.BusinessException;
import com.lawfirm.repository.KnowledgeArticleRepository;
import com.lawfirm.repository.RagEvaluationCaseRepository;
import com.lawfirm.repository.RagEvaluationRunRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RagEvaluationServiceTest {

    @Test
    void createsEvaluationCaseOnlyForIndexableExpectedArticles() {
        RagEvaluationCaseRepository caseRepository = mock(RagEvaluationCaseRepository.class);
        RagEvaluationRunRepository runRepository = mock(RagEvaluationRunRepository.class);
        KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
        RAGKnowledgeService ragService = mock(RAGKnowledgeService.class);
        RagEvaluationService service = new RagEvaluationService(
                caseRepository, runRepository, articleRepository, ragService);
        KnowledgeArticle article = article(7L, true);
        when(articleRepository.findAllById(any())).thenReturn(List.of(article));
        when(caseRepository.save(any())).thenAnswer(invocation -> {
            RagEvaluationCase saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        RagEvaluationCaseRequest request = request(List.of(7L), List.of());

        RagEvaluationCaseDTO created = service.createCase(request, 3L);

        assertEquals(10L, created.getId());
        assertEquals(List.of(7L), created.getExpectedArticleIds());
        assertEquals(3L, created.getCreatedBy());
    }

    @Test
    void rejectsPrivateOrUnreviewedExpectedArticle() {
        RagEvaluationCaseRepository caseRepository = mock(RagEvaluationCaseRepository.class);
        KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
        when(articleRepository.findAllById(any())).thenReturn(List.of(article(7L, false)));
        RagEvaluationService service = new RagEvaluationService(
                caseRepository, mock(RagEvaluationRunRepository.class), articleRepository,
                mock(RAGKnowledgeService.class));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createCase(request(List.of(7L), List.of()), 3L));

        assertEquals(400, error.getCode());
        verify(caseRepository, never()).save(any());
    }

    @Test
    void candidateListExposesOnlyMetadataAndMarksPrivateDocumentsNonIndexable() {
        KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
        when(articleRepository.findAll()).thenReturn(List.of(article(1L, true), article(2L, false)));
        RagEvaluationService service = new RagEvaluationService(
                mock(RagEvaluationCaseRepository.class), mock(RagEvaluationRunRepository.class),
                articleRepository, mock(RAGKnowledgeService.class));

        List<Map<String, Object>> candidates = service.listCandidateArticles();

        assertEquals(2, candidates.size());
        assertEquals(false, candidates.get(0).get("ragIndexable"));
        assertFalse(candidates.get(0).containsKey("content"));
    }

    @Test
    void runReportsTop3HitAndPersistsAuditableResult() {
        RagEvaluationCaseRepository caseRepository = mock(RagEvaluationCaseRepository.class);
        RagEvaluationRunRepository runRepository = mock(RagEvaluationRunRepository.class);
        RAGKnowledgeService ragService = mock(RAGKnowledgeService.class);
        RagEvaluationCase evaluationCase = evaluationCase("劳动时效", "1", "9");
        when(caseRepository.findByDeletedFalseAndEnabledTrueOrderByCreatedAtAsc())
                .thenReturn(List.of(evaluationCase));
        when(ragService.evaluateRetrieval("劳动仲裁时效多久"))
                .thenReturn(new RAGKnowledgeService.RetrievalSnapshot(List.of(2L, 1L, 3L), "KEYWORD", 18));
        when(runRepository.save(any())).thenAnswer(invocation -> {
            RagEvaluationRun run = invocation.getArgument(0);
            run.setId(22L);
            return run;
        });
        RagEvaluationService service = new RagEvaluationService(
                caseRepository, runRepository, mock(KnowledgeArticleRepository.class), ragService);

        Map<String, Object> result = service.runEnabledCases(5L);

        assertEquals(1, result.get("total"));
        assertEquals(1L, result.get("passed"));
        assertEquals(100.0, result.get("top3HitRate"));
        assertEquals(true, result.get("privacyBoundaryPassed"));
        verify(runRepository).save(argThat(run -> Boolean.TRUE.equals(run.getPassed())
                && "2,1,3".equals(run.getRetrievedArticleIds()) && run.getRunBy().equals(5L)));
    }

    @Test
    void forbiddenHitFailsPrivacyBoundaryEvenWhenExpectedDocumentMatches() {
        RagEvaluationCaseRepository caseRepository = mock(RagEvaluationCaseRepository.class);
        RagEvaluationRunRepository runRepository = mock(RagEvaluationRunRepository.class);
        RAGKnowledgeService ragService = mock(RAGKnowledgeService.class);
        when(caseRepository.findByDeletedFalseAndEnabledTrueOrderByCreatedAtAsc())
                .thenReturn(List.of(evaluationCase("边界检查", "1", "9")));
        when(ragService.evaluateRetrieval(anyString()))
                .thenReturn(new RAGKnowledgeService.RetrievalSnapshot(List.of(1L, 9L), "VECTOR", 7));
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RagEvaluationService service = new RagEvaluationService(
                caseRepository, runRepository, mock(KnowledgeArticleRepository.class), ragService);

        Map<String, Object> result = service.runEnabledCases(5L);

        assertEquals(1L, result.get("forbiddenHitCount"));
        assertEquals(false, result.get("privacyBoundaryPassed"));
        assertEquals(0L, result.get("passed"));
    }

    private RagEvaluationCaseRequest request(List<Long> expected, List<Long> forbidden) {
        RagEvaluationCaseRequest request = new RagEvaluationCaseRequest();
        request.setName("劳动争议测试");
        request.setQuestion("劳动仲裁时效多久");
        request.setExpectedArticleIds(expected);
        request.setForbiddenArticleIds(forbidden);
        return request;
    }

    private RagEvaluationCase evaluationCase(String name, String expected, String forbidden) {
        RagEvaluationCase evaluationCase = new RagEvaluationCase();
        evaluationCase.setId(11L);
        evaluationCase.setName(name);
        evaluationCase.setQuestion("劳动仲裁时效多久");
        evaluationCase.setExpectedArticleIds(expected);
        evaluationCase.setForbiddenArticleIds(forbidden);
        evaluationCase.setEnabled(true);
        evaluationCase.setCreatedBy(3L);
        return evaluationCase;
    }

    private KnowledgeArticle article(Long id, boolean indexable) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(id);
        article.setKnowledgeSource(indexable ? "LAW_REGULATION" : "CASE_DEPOSIT");
        article.setKnowledgeEligible(indexable);
        article.setIsPublic(indexable);
        article.setReviewStatus(indexable ? "APPROVED" : "PENDING_REVIEW");
        article.setValidityStatus("EFFECTIVE");
        article.setDeleted(false);
        return article;
    }
}
