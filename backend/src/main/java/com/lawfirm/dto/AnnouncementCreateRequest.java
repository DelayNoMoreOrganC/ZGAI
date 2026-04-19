package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公告创建请求
 */
@Data
public class AnnouncementCreateRequest {

    @NotBlank(message = "公告标题不能为空")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    private String targetScope = "ALL";

    private Integer priority = 0;

    private String attachments;
}
