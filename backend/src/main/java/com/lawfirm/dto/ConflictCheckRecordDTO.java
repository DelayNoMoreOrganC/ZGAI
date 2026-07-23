package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 利冲检查历史摘要。
 */
@Data
public class ConflictCheckRecordDTO {

    private Long id;
    private String reportNo;
    private String subjectName;
    private Long caseId;
    private String checkedByName;
    private String conflictLevel;
    private String conclusion;
    private String similarNames;
    private Integer matchedClientCount;
    private Integer matchedCaseCount;
    private Integer matchedRelatedSubjectCount;
    private String matchedRelatedSubjects;
    private LocalDateTime checkedAt;
    private String reviewStatus;
    private String reviewDecision;
    private String reviewConclusion;
    private String waiverBasis;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private Long archivedDocumentId;
    private LocalDateTime archivedAt;
    private Boolean canDownload;
    private List<ConflictWaiverAttachmentDTO> waiverAttachments;
}
