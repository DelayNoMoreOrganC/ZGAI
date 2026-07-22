package com.lawfirm.service;

import com.lawfirm.entity.KnowledgeArticle;

import java.util.Locale;
import java.util.Set;

/**
 * Shared safety policy for knowledge-library visibility and RAG admission.
 */
public final class KnowledgeArticlePolicy {

    public static final String LAW_REGULATION = "LAW_REGULATION";
    public static final String FIRM_POLICY = "FIRM_POLICY";
    public static final String PUBLIC_TEMPLATE = "PUBLIC_TEMPLATE";
    public static final String REFERENCE_MATERIAL = "REFERENCE_MATERIAL";
    public static final String FIRM_KNOWLEDGE = "FIRM_KNOWLEDGE";
    public static final String CASE_DEPOSIT = "CASE_DEPOSIT";

    public static final String VALIDITY_EFFECTIVE = "EFFECTIVE";
    public static final String VALIDITY_AMENDED = "AMENDED";
    public static final String VALIDITY_REPEALED = "REPEALED";
    public static final String VALIDITY_UNKNOWN = "UNKNOWN";

    private static final Set<String> ALLOWED_SOURCES = Set.of(
            LAW_REGULATION,
            FIRM_POLICY,
            PUBLIC_TEMPLATE,
            REFERENCE_MATERIAL,
            FIRM_KNOWLEDGE,
            CASE_DEPOSIT
    );

    private static final Set<String> SHARED_RAG_SOURCES = Set.of(
            LAW_REGULATION,
            FIRM_POLICY,
            PUBLIC_TEMPLATE,
            REFERENCE_MATERIAL,
            FIRM_KNOWLEDGE
    );

    private static final Set<String> VALIDITY_STATUSES = Set.of(
            VALIDITY_EFFECTIVE,
            VALIDITY_AMENDED,
            VALIDITY_REPEALED,
            VALIDITY_UNKNOWN
    );

    private KnowledgeArticlePolicy() {
    }

    public static String normalizeSource(String source) {
        String normalized = normalizeCode(source);
        if (!ALLOWED_SOURCES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的知识来源：" + source);
        }
        return normalized;
    }

    public static String normalizeValidityStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return VALIDITY_UNKNOWN;
        }
        String normalized = normalizeCode(status);
        if (!VALIDITY_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的知识有效状态：" + status);
        }
        return normalized;
    }

    public static boolean isSharedRagSource(String source) {
        return source == null || SHARED_RAG_SOURCES.contains(source);
    }

    public static boolean requiresAuthorization(String source) {
        return REFERENCE_MATERIAL.equals(source);
    }

    public static boolean isRagIndexable(KnowledgeArticle article) {
        if (article == null || Boolean.TRUE.equals(article.getDeleted())) {
            return false;
        }
        if (!Boolean.TRUE.equals(article.getKnowledgeEligible())
                || !Boolean.TRUE.equals(article.getIsPublic())
                || !isSharedRagSource(article.getKnowledgeSource())) {
            return false;
        }
        if (VALIDITY_REPEALED.equals(normalizeValidityStatus(article.getValidityStatus()))) {
            return false;
        }
        return !requiresAuthorization(article.getKnowledgeSource())
                || Boolean.TRUE.equals(article.getAuthorizationConfirmed());
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
