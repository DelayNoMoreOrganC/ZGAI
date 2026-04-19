package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String category;
    private String tags;
    private String summary;
    private String content;
    private String attachmentPath;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isTop;
    private Boolean isPublic;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
