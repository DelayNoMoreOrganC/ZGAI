package com.lawfirm.service;

import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.repository.KnowledgeArticleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RAGKnowledgeServiceTest {

    @Test
    void startupSkipsQdrantWhenEmbeddingIsNotConfigured() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantVectorService qdrantVectorService = mock(QdrantVectorService.class);
        when(embeddingService.isConfigured()).thenReturn(false);
        RAGKnowledgeService service = service(embeddingService, qdrantVectorService);

        service.init();

        verify(qdrantVectorService, never()).initializeCollection();
        verify(qdrantVectorService, never()).isEnabled();
    }

    @Test
    void startupInitializesQdrantOnlyWhenVectorStackIsEnabled() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantVectorService qdrantVectorService = mock(QdrantVectorService.class);
        when(embeddingService.isConfigured()).thenReturn(true);
        when(qdrantVectorService.isEnabled()).thenReturn(true);
        RAGKnowledgeService service = service(embeddingService, qdrantVectorService);

        service.init();

        verify(qdrantVectorService).initializeCollection();
    }

    @Test
    void configuredEmbeddingDoesNotPretendVectorReadyWhenQdrantIsOffline() {
        AIConfigService aiConfigService = mock(AIConfigService.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantVectorService qdrantVectorService = mock(QdrantVectorService.class);
        when(aiConfigService.getUsableDefaultConfigOrNull()).thenReturn(new AIConfig());
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.healthStatus()).thenReturn(Map.of(
                "status", "configured", "configuredDimension", 1024));
        when(qdrantVectorService.healthStatus()).thenReturn(Map.of(
                "status", "unavailable", "configuredDimension", 1024));
        RAGKnowledgeService service = new RAGKnowledgeService(
                aiConfigService, mock(KnowledgeArticleRepository.class), embeddingService,
                qdrantVectorService, mock(OpenAICompatibleClient.class), mock(AIGenerationGateway.class));

        Map<String, Object> status = service.healthStatus();

        assertEquals("DEGRADED", status.get("status"));
        assertEquals("KEYWORD", status.get("mode"));
    }

    @Test
    void vectorReadyRequiresEmbeddingQdrantAndLlmTogether() {
        AIConfigService aiConfigService = mock(AIConfigService.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantVectorService qdrantVectorService = mock(QdrantVectorService.class);
        when(aiConfigService.getUsableDefaultConfigOrNull()).thenReturn(new AIConfig());
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.healthStatus()).thenReturn(Map.of(
                "status", "configured", "configuredDimension", 1024));
        when(qdrantVectorService.healthStatus()).thenReturn(Map.of(
                "status", "ready", "configuredDimension", 1024));
        RAGKnowledgeService service = new RAGKnowledgeService(
                aiConfigService, mock(KnowledgeArticleRepository.class), embeddingService,
                qdrantVectorService, mock(OpenAICompatibleClient.class), mock(AIGenerationGateway.class));

        Map<String, Object> status = service.healthStatus();

        assertEquals("READY", status.get("status"));
        assertEquals("VECTOR_READY", status.get("mode"));
    }

    @Test
    void chineseQuestionFallsBackToKeywordRetrievalWithoutPretendingLlmAnswer() {
        AIConfigService aiConfigService = mock(AIConfigService.class);
        KnowledgeArticleRepository repository = mock(KnowledgeArticleRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantVectorService qdrantVectorService = mock(QdrantVectorService.class);
        RAGKnowledgeService service = new RAGKnowledgeService(
                aiConfigService, repository, embeddingService, qdrantVectorService,
                mock(OpenAICompatibleClient.class), mock(AIGenerationGateway.class));
        KnowledgeArticle article = publicArticle(1L, "广东省律师防止利益冲突规则",
                "第一章 总则\n第一条 本规则用于防止利益冲突。\n" +
                "第二章 利益冲突的认定\n第一节 直接利益冲突\n" +
                "第五条 同一律师事务所不得在同一案件中同时担任争议双方当事人的代理人。\n" +
                "第二节 间接利益冲突\n第十二条 委托关系终止后一定期限内存在间接利益冲突。");
        when(embeddingService.isConfigured()).thenReturn(false);
        when(aiConfigService.getUsableDefaultConfigOrNull()).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(article));

        Map<String, Object> result = service.ragSearch("利益冲突有哪些直接情形", 9L);

        assertEquals("KEYWORD", result.get("searchMethod"));
        assertEquals("RETRIEVAL_ONLY", result.get("answerMode"));
        assertEquals(true, result.get("hasAnswer"));
        assertTrue(String.valueOf(result.get("answer")).contains("本次未生成AI综合回答"));
        assertTrue(String.valueOf(result.get("answer")).contains("第五条"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sources = (List<Map<String, Object>>) result.get("sources");
        assertTrue(String.valueOf(sources.get(0).get("excerpt")).contains("直接利益冲突"));
    }

    @Test
    void privateCaseMaterialIsExcludedFromFallbackRetrieval() {
        AIConfigService aiConfigService = mock(AIConfigService.class);
        KnowledgeArticleRepository repository = mock(KnowledgeArticleRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantVectorService qdrantVectorService = mock(QdrantVectorService.class);
        RAGKnowledgeService service = new RAGKnowledgeService(
                aiConfigService, repository, embeddingService, qdrantVectorService,
                mock(OpenAICompatibleClient.class), mock(AIGenerationGateway.class));
        KnowledgeArticle privateCase = publicArticle(2L, "某案件材料", "客户隐私与案件证据");
        privateCase.setKnowledgeSource("CASE_DEPOSIT");
        privateCase.setIsPublic(false);
        privateCase.setKnowledgeEligible(false);
        when(embeddingService.isConfigured()).thenReturn(false);
        when(repository.findAll()).thenReturn(List.of(privateCase));

        Map<String, Object> result = service.ragSearch("客户隐私案件证据", 9L);

        assertEquals(false, result.get("hasAnswer"));
        assertFalse(String.valueOf(result.get("answer")).contains("某案件材料"));
    }

    @Test
    void evaluationSnapshotUsesRetrievalOnlyAndKeepsPrivateMaterialOut() {
        AIConfigService aiConfigService = mock(AIConfigService.class);
        KnowledgeArticleRepository repository = mock(KnowledgeArticleRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantVectorService qdrantVectorService = mock(QdrantVectorService.class);
        AIGenerationGateway generationGateway = mock(AIGenerationGateway.class);
        RAGKnowledgeService service = new RAGKnowledgeService(
                aiConfigService, repository, embeddingService, qdrantVectorService,
                mock(OpenAICompatibleClient.class), generationGateway);
        KnowledgeArticle publicRule = publicArticle(1L, "劳动争议调解仲裁法", "劳动争议申请仲裁的时效期间为一年");
        KnowledgeArticle privateCase = publicArticle(9L, "客户案件记录", "劳动争议申请仲裁的时效期间为一年");
        privateCase.setKnowledgeSource("CASE_DEPOSIT");
        privateCase.setKnowledgeEligible(false);
        privateCase.setIsPublic(false);
        when(embeddingService.isConfigured()).thenReturn(false);
        when(repository.findAll()).thenReturn(List.of(publicRule, privateCase));

        RAGKnowledgeService.RetrievalSnapshot snapshot = service.evaluateRetrieval("劳动仲裁时效");

        assertEquals(List.of(1L), snapshot.getArticleIds());
        assertEquals("KEYWORD", snapshot.getSearchMethod());
        verify(generationGateway, never()).generate(any(), any(), anyInt());
    }

    @Test
    void retrievalLocatesTimeLimitAndHandlingProvisionsWithoutHardcodedArticleNumbers() {
        AIConfigService aiConfigService = mock(AIConfigService.class);
        KnowledgeArticleRepository repository = mock(KnowledgeArticleRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantVectorService qdrantVectorService = mock(QdrantVectorService.class);
        RAGKnowledgeService service = new RAGKnowledgeService(
                aiConfigService, repository, embeddingService, qdrantVectorService,
                mock(OpenAICompatibleClient.class), mock(AIGenerationGateway.class));
        KnowledgeArticle article = publicArticle(3L, "利益冲突规则",
                "第十二条 委托关系终止后的一定时间内可能构成间接利益冲突：\n" +
                "（一）其他普通业务关系；\n" +
                "（三）委托业务终结或者委托关系终止后十二个月以内不得接受相关委托；\n" +
                "（四）委托关系终止后六个月内同所其他律师不得接受相关委托。\n" +
                "第三章 利益冲突的处理\n" +
                "第十四条 遇有直接利益冲突应当主动回避，不得接受委托，已经接受的应当解除。\n" +
                "第十六条 间接利益冲突应当征得书面同意。\n");
        when(embeddingService.isConfigured()).thenReturn(false);
        when(aiConfigService.getUsableDefaultConfigOrNull()).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(article));

        String timeLimitAnswer = String.valueOf(service.ragSearch(
                "委托关系终止后多久可能仍构成间接利益冲突？", 9L).get("answer"));
        String handlingAnswer = String.valueOf(service.ragSearch(
                "发现直接利益冲突后应当如何处理？", 9L).get("answer"));

        assertTrue(timeLimitAnswer.contains("十二个月"));
        assertTrue(timeLimitAnswer.contains("六个月"));
        assertTrue(handlingAnswer.contains("主动回避"));
        assertTrue(handlingAnswer.contains("解除"));
    }

    private KnowledgeArticle publicArticle(Long id, String title, String content) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(id);
        article.setTitle(title);
        article.setContent(content);
        article.setKnowledgeSource("LAW_REGULATION");
        article.setKnowledgeEligible(true);
        article.setIsPublic(true);
        article.setDeleted(false);
        article.setIndexStatus("NOT_INDEXED");
        return article;
    }

    private RAGKnowledgeService service(EmbeddingService embeddingService,
                                        QdrantVectorService qdrantVectorService) {
        return new RAGKnowledgeService(
                mock(AIConfigService.class), mock(KnowledgeArticleRepository.class), embeddingService,
                qdrantVectorService, mock(OpenAICompatibleClient.class), mock(AIGenerationGateway.class));
    }
}
