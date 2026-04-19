package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 案件动态DTO
 */
@Data
public class CaseTimelineDTO {

    /**
     * 动态ID（更新时使用）
     */
    private Long id;

    /**
     * 操作类型（COMMENT/STATUS_CHANGE/RECORD_ADDED等）
     */
    @NotBlank(message = "操作类型不能为空")
    private String actionType;

    /**
     * 操作内容
     */
    @NotBlank(message = "操作内容不能为空")
    private String actionContent;

    /**
     * 是否为评论
     */
    private Boolean isComment = false;

    /**
     * 父级评论ID（回复评论时使用）
     */
    private Long parentId;

    /**
     * @提及的用户ID列表
     */
    private List<Long> mentionIds;
}
