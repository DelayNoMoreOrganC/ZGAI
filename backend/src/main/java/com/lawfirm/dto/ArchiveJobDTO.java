package com.lawfirm.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ArchiveJobDTO {
    private Long id;
    private Long caseId;
    private String caseName;
    private String caseNumber;
    private String status;
    private String templateVersion;
    private Integer progress;
    private String currentStage;
    private String errorMessage;
    private String reviewReason;
    private String exceptionReason;
    private String correctionReason;
    private Long createdBy;
    private String createdByName;
    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime completedAt;
    private boolean canEdit;
    private boolean canSubmit;
    private boolean canReview;
    private boolean canDownload;
    private List<String> missingCritical = new ArrayList<>();
    private List<DocumentItem> documents = new ArrayList<>();
    private List<FieldItem> fields = new ArrayList<>();
    private OutputItem output;

    @Data
    public static class DocumentItem {
        private Long id;
        private Long caseDocumentId;
        private String originalFileName;
        private Integer catalogSeq;
        private String catalogName;
        private String documentType;
        private Boolean included;
        private Integer sourcePageCount;
        private Integer outputStartPage;
        private Integer outputEndPage;
        private String contentSha256;
        private Double confidence;
        private String classificationReason;
    }

    @Data
    public static class FieldItem {
        private String key;
        private String value;
        private Long sourceDocumentId;
        private Integer sourcePage;
        private Double confidence;
        private String extractionReason;
        private boolean confirmed;
    }

    @Data
    public static class OutputItem {
        private Long id;
        private Integer versionNo;
        private String fileName;
        private String contentSha256;
        private String manifestSha256;
        private Integer pageCount;
        private Integer sourcePageCount;
        private Integer gapPages;
        private Integer duplicatePages;
    }
}
