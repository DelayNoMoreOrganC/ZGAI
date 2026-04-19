package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告DTO
 */
@Data
public class AnnouncementDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime publishDate;
    private Long publisherId;
    private String publisherName;
    private String targetScope;
    private String targetScopeDesc;
    private Integer priority;
    private String attachments;
    private Integer readCount;
    private Boolean isRead;
}
