package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 知识库文章VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticleVO {

    private Long id;
    private String title;
    private String articleType;
    private String knowledgeSource;
    private String category;
    private String tags;
    private String summary;
    private String content;
    private String attachmentPath;
    private String attachmentName;
    private String sourceReference;
    private String issuingAuthority;
    private String documentNumber;
    private LocalDate effectiveDate;
    private String validityStatus;
    private Boolean authorizationConfirmed;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isTop;
    private Boolean isPublic;
    private Boolean knowledgeEligible;
    private String indexStatus;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
