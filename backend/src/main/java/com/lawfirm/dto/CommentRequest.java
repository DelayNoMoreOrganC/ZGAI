package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 评论请求DTO
 */
@Data
public class CommentRequest {

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    private String content;

    /**
     * @提及的用户ID列表
     */
    private List<Long> mentionIds;

    /**
     * 父评论ID（用于回复评论）
     */
    private Long parentId;
}
