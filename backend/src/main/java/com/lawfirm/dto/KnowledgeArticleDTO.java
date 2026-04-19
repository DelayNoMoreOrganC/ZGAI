package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 创建/更新知识库文章DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticleDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "文章类型不能为空")
    private String articleType;

    private String category;

    private String tags;

    private String summary;

    private String content;

    private String attachmentPath;

    private Boolean isPublic;

    private Boolean isTop;
}
