package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * OCR提取请求DTO
 */
public class OcrExtractRequest {

    @NotBlank(message = "OCR文本不能为空")
    @Size(max = 60000, message = "OCR文本不能超过60000个字符")
    private String ocrText;

    private Long caseId;

    @Size(max = 40, message = "文书类型不能超过40个字符")
    private String documentType;

    @Size(max = 40, message = "模型类型不能超过40个字符")
    private String providerType;

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

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }
}
