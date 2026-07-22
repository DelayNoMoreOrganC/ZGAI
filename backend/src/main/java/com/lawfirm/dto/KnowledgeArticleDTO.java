package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;

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

    private String knowledgeSource;

    private String category;

    private String tags;

    private String summary;

    private String content;

    private String attachmentPath;

    @Size(max = 500, message = "来源依据不能超过500个字符")
    private String sourceReference;

    @Size(max = 200, message = "发布机关不能超过200个字符")
    private String issuingAuthority;

    @Size(max = 100, message = "文号不能超过100个字符")
    private String documentNumber;

    private LocalDate effectiveDate;

    private String validityStatus;

    private Boolean authorizationConfirmed;

    private Boolean isPublic;

    private Boolean isTop;

    private Boolean knowledgeEligible;

    private String indexStatus;
}
