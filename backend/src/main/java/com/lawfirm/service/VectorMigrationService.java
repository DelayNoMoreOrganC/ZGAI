package com.lawfirm.service;

import com.google.gson.JsonObject;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库向量迁移服务
 *
 * 功能：
 * 1. 启动时自动检查并迁移现有知识库文章到Qdrant
 * 2. 为新创建的文章自动生成向量
 * 3. 支持手动触发全量迁移
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Order(1) // 确保在其他初始化之后执行
public class VectorMigrationService {

    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final EmbeddingService embeddingService;
    private final QdrantVectorService qdrantVectorService;

    /**
     * 应用启动时自动迁移（仅在dev环境执行）
     */
    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // @PostConstruct
    public void migrateOnStartup() {
        if ("dev".equals(activeProfile)) {
            log.info("检测到dev环境，跳过自动向量迁移（避免重复）");
            return;
        }

        try {
            migrateExistingArticles();
        } catch (Exception e) {
            log.warn("启动时向量迁移失败（可忽略）: {}", e.getMessage());
        }
    }

    /**
     * 迁移现有知识库文章到向量数据库
     *
     * @return 迁移成功的文章数量
     */
    public int migrateExistingArticles() {
        log.info("开始迁移现有知识库文章到向量数据库...");

        try {
            // 获取所有文章
            List<KnowledgeArticle> articles = knowledgeArticleRepository.findAll();
            if (articles.isEmpty()) {
                log.info("知识库为空，无需迁移");
                return 0;
            }

            log.info("找到 {} 篇文章需要迁移", articles.size());

            // 批量生成向量（每次25篇）
            int batchSize = 25;
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < articles.size(); i += batchSize) {
                int end = Math.min(i + batchSize, articles.size());
                List<KnowledgeArticle> batch = articles.subList(i, end);

                try {
                    int migrated = migrateBatch(batch);
                    successCount += migrated;
                    failCount += (batch.size() - migrated);
                } catch (Exception e) {
                    log.error("批次迁移失败: {}-{}", i, end, e);
                    failCount += batch.size();
                }

                // 避免API限流
                if (i + batchSize < articles.size()) {
                    Thread.sleep(1000);
                }
            }

            log.info("向量迁移完成: 成功={}, 失败={}, 总计={}", successCount, failCount, articles.size());
            return successCount;

        } catch (Exception e) {
            log.error("向量迁移失败", e);
            throw new RuntimeException("向量迁移失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量迁移文章
     */
    private int migrateBatch(List<KnowledgeArticle> articles) {
        if (articles.isEmpty()) {
            return 0;
        }

        try {
            // 1. 准备文本内容
            List<String> texts = new ArrayList<>();
            for (KnowledgeArticle article : articles) {
                String text = extractTextForEmbedding(article);
                texts.add(text);
            }

            // 2. 批量生成向量
            List<List<Double>> embeddings = embeddingService.embedTextBatch(texts);

            // 3. 批量插入Qdrant
            List<QdrantVectorService.VectorPoint> points = new ArrayList<>();
            for (int i = 0; i < articles.size(); i++) {
                KnowledgeArticle article = articles.get(i);
                List<Double> embedding = embeddings.get(i);

                // 构建payload（存储元数据）
                JsonObject payload = new JsonObject();
                payload.addProperty("articleId", article.getId());
                payload.addProperty("title", article.getTitle());
                payload.addProperty("category", article.getCategory());
                payload.addProperty("createdAt", article.getCreatedAt().toString());

                points.add(new QdrantVectorService.VectorPoint(
                        article.getId(),
                        embedding,
                        payload
                ));
            }

            qdrantVectorService.insertPointsBatch(points);
            log.debug("成功迁移批次: {} 篇文章", articles.size());

            return articles.size();

        } catch (Exception e) {
            log.error("批次迁移失败: {} 篇文章", articles.size(), e);
            throw new RuntimeException("批次迁移失败", e);
        }
    }

    /**
     * 为新创建的文章生成向量
     *
     * @param article 新文章
     */
    public void indexNewArticle(KnowledgeArticle article) {
        if (article == null || article.getId() == null) {
            log.warn("文章无效，无法生成向量");
            return;
        }

        try {
            // 提取文本
            String text = extractTextForEmbedding(article);

            // 生成向量
            List<Double> embedding = embeddingService.embedText(text);

            // 构建payload
            JsonObject payload = new JsonObject();
            payload.addProperty("articleId", article.getId());
            payload.addProperty("title", article.getTitle());
            payload.addProperty("category", article.getCategory());
            payload.addProperty("createdAt", article.getCreatedAt().toString());

            // 插入Qdrant
            qdrantVectorService.insertPoint(article.getId(), embedding, payload);

            log.info("成功为新文章生成向量: id={}, title={}", article.getId(), article.getTitle());

        } catch (Exception e) {
            log.error("为新文章生成向量失败: id={}", article.getId(), e);
            // 不抛出异常，避免影响文章创建流程
        }
    }

    /**
     * 删除文章的向量索引
     *
     * @param articleId 文章ID
     */
    public void deleteArticleIndex(Long articleId) {
        if (articleId == null) {
            return;
        }

        try {
            qdrantVectorService.deletePoint(articleId);
            log.info("成功删除文章向量索引: id={}", articleId);
        } catch (Exception e) {
            log.warn("删除文章向量索引失败: id={}", articleId, e);
        }
    }

    /**
     * 提取文章文本用于生成向量
     */
    private String extractTextForEmbedding(KnowledgeArticle article) {
        StringBuilder text = new StringBuilder();

        if (article.getTitle() != null) {
            text.append(article.getTitle()).append("\n");
        }

        if (article.getSummary() != null) {
            text.append(article.getSummary()).append("\n");
        }

        if (article.getContent() != null) {
            text.append(article.getContent());
        }

        String result = text.toString();

        // 限制文本长度（阿里云API最大2048字符）
        if (result.length() > 2000) {
            result = result.substring(0, 2000);
        }

        return result;
    }
}
