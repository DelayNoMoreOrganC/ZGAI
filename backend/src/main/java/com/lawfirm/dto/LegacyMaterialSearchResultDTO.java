package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 旧系统资料检索结果。
 */
@Data
public class LegacyMaterialSearchResultDTO {

    private String sourceType;

    private Long relatedId;

    private String title;

    private String caseNumber;

    private String clientName;

    private String caseReason;

    private String ownerName;

    private String departmentName;

    private Long legacyFileId;

    private Long fileSize;

    private LocalDateTime lastModifiedAt;

    private Boolean downloadable = false;

    private String matchReason;

    private Double score;
}
