package com.lawfirm.service;

import com.lawfirm.entity.KnowledgeArticle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeArticlePolicyTest {

    @Test
    void externalReferenceRequiresConfirmedInternalUseAuthorization() {
        KnowledgeArticle article = indexableArticle(KnowledgeArticlePolicy.REFERENCE_MATERIAL);

        assertFalse(KnowledgeArticlePolicy.isRagIndexable(article));

        article.setAuthorizationConfirmed(true);
        assertTrue(KnowledgeArticlePolicy.isRagIndexable(article));
    }

    @Test
    void repealedRegulationIsExcludedFromRag() {
        KnowledgeArticle article = indexableArticle(KnowledgeArticlePolicy.LAW_REGULATION);
        article.setValidityStatus(KnowledgeArticlePolicy.VALIDITY_REPEALED);

        assertFalse(KnowledgeArticlePolicy.isRagIndexable(article));
    }

    @Test
    void effectivePublicRegulationRemainsIndexable() {
        KnowledgeArticle article = indexableArticle(KnowledgeArticlePolicy.LAW_REGULATION);
        article.setValidityStatus(KnowledgeArticlePolicy.VALIDITY_EFFECTIVE);

        assertTrue(KnowledgeArticlePolicy.isRagIndexable(article));
    }

    private KnowledgeArticle indexableArticle(String source) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setKnowledgeSource(source);
        article.setKnowledgeEligible(true);
        article.setIsPublic(true);
        article.setDeleted(false);
        article.setValidityStatus(KnowledgeArticlePolicy.VALIDITY_UNKNOWN);
        article.setAuthorizationConfirmed(false);
        return article;
    }
}
