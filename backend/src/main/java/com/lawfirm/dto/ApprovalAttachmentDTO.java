package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalAttachmentDTO {
    private Long id;
    private Long approvalId;
    private Long caseDocumentId;
    private String originalFileName;
    private Long fileSize;
    private String mimeType;
    private String contentSha256;
    private String sourceType;
    private String sealStatus;
    private Long uploadedBy;
    private String uploadedByName;
    private Long decidedBy;
    private String decidedByName;
    private LocalDateTime decidedAt;
}
