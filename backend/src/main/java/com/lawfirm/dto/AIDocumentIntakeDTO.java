package com.lawfirm.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class AIDocumentIntakeDTO {
    private Long id;
    private String originalFileName;
    private String mimeType;
    private Long fileSize;
    private String contentSha256;
    private String status;
    private Map<String, Object> analysis;
    private List<DocumentMatchCandidateDTO> candidates = new ArrayList<>();
    private String suggestedFolder;
    private String suggestedDocumentType;
    private Long confirmedCaseId;
    private Long caseDocumentId;
    private LocalDateTime expiresAt;
    private String message;
}
