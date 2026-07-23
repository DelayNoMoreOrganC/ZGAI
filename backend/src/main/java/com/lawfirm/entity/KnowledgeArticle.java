package com.lawfirm.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 知识库文章实体
 */
@Entity
@Table(name = "knowledge_article", indexes = {
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_type", columnList = "article_type"),
    @Index(name = "idx_tags", columnList = "tags"),
    @Index(name = "idx_knowledge_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 文章类型：TEMPLATE(模板), CASE(类案), GUIDE(指南), EXPERIENCE(经验)
     */
    @NotBlank(message = "文章类型不能为空")
    @Column(name = "article_type", nullable = false, length = 20)
    private String articleType;

    /**
     * 知识来源：FIRM_KNOWLEDGE(全所知识), PUBLIC_TEMPLATE(公共模板), CASE_DEPOSIT(案件沉淀)
     */
    @Column(name = "knowledge_source", length = 30)
    private String knowledgeSource = "FIRM_KNOWLEDGE";

    /**
     * 分类：合同、劳动、侵权等
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 标签（逗号分隔）
     */
    @Column(name = "tags", length = 500)
    private String tags;

    /**
     * 摘要
     */
    @Column(name = "summary", length = 1000)
    private String summary;

    /**
     * 内容（富文本）
     */
    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 附件路径
     */
    @Column(name = "attachment_path")
    private String attachmentPath;

    /**
     * Official URL, internal document reference, or licensed source description.
     */
    @Column(name = "source_reference", length = 500)
    private String sourceReference;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "source_relative_path", length = 1000)
    private String sourceRelativePath;

    @Column(name = "content_sha256", length = 64)
    private String contentSha256;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    /** APPROVED, PENDING_REVIEW or REJECTED. */
    @Column(name = "review_status", length = 30)
    private String reviewStatus = "APPROVED";

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_reason", length = 1000)
    private String reviewReason;

    @Column(name = "issuing_authority", length = 200)
    private String issuingAuthority;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /**
     * EFFECTIVE, AMENDED, REPEALED, UNKNOWN.
     */
    @Column(name = "validity_status", length = 20)
    private String validityStatus = "UNKNOWN";

    /**
     * Required before externally sourced reference material may enter RAG.
     */
    @Column(name = "authorization_confirmed")
    private Boolean authorizationConfirmed = false;

    @Column(name = "knowledge_eligible")
    private Boolean knowledgeEligible = true;

    @Column(name = "index_status", length = 30)
    private String indexStatus = "PENDING";

    /**
     * 浏览次数
     */
    @Column(name = "view_count")
    private Integer viewCount = 0;

    /**
     * 点赞次数
     */
    @Column(name = "like_count")
    private Integer likeCount = 0;

    /**
     * 是否置顶
     */
    @Column(name = "is_top")
    private Boolean isTop = false;

    /**
     * 是否公开（全所可见）
     */
    @Column(name = "is_public")
    private Boolean isPublic = true;

    /**
     * 作者ID
     */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /**
     * 作者姓名（冗余字段，避免关联查询）
     */
    @Column(name = "author_name", length = 50)
    private String authorName;

    /**
     * 最后修改人ID
     */
    @Column(name = "updater_id")
    private Long updaterId;

    /**
     * 最后修改时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
