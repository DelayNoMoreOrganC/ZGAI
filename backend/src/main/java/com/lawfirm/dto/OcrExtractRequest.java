package com.lawfirm.dto;

import javax.validation.constraints.NotNull;

/**
 * OCR提取请求DTO
 */
public class OcrExtractRequest {

    @NotNull(message = "OCR文本不能为空")
    private String ocrText;

    private Long caseId;

    private String documentType;

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
}
