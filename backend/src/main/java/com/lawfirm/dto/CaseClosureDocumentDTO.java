package com.lawfirm.dto;

import lombok.Data;

@Data
public class CaseClosureDocumentDTO {
    private Long documentId;
    private String documentName;
    private String documentType;
    private String folderPath;
}
