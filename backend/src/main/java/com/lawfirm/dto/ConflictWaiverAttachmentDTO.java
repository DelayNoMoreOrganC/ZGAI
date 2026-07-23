package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConflictWaiverAttachmentDTO {

    private Long id;
    private Long conflictCheckRecordId;
    private Long caseId;
    private String originalFileName;
    private Long fileSize;
    private String mimeType;
    private String contentSha256;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
    private Long archivedDocumentId;
    private LocalDateTime archivedAt;
}
